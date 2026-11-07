import { test } from 'node:test';
import assert from 'node:assert/strict';
import { promises as fs } from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { Album, makeServer, selectForecast, weatherView, fetchWeather } from '../server/server.mjs';
const png = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aX2kAAAAASUVORK5CYII=', 'base64');
async function fixture(t) {
  const dir = await fs.mkdtemp(path.join(os.tmpdir(),'ambient-test-'));
  t.after(()=>fs.rm(dir,{recursive:true,force:true}));
  const source = path.join(dir,'source'); await fs.mkdir(source);
  const album = new Album(source,path.join(dir,'data')); await album.load();
  return {album,source};
}
test('photo import is content-addressed, deduplicated and survives restart',async t=>{
  const {album,source}=await fixture(t);
  await fs.writeFile(path.join(source,'a.png'),png); await fs.writeFile(path.join(source,'b.png'),png);
  await album.sync(); assert.equal(album.manifest.photos.length,1);
  const next=new Album(source,album.data); await next.load();
  assert.deepEqual(next.manifest,album.manifest);
});
test('failed or empty synchronisation keeps the previous album',async t=>{
  const {album,source}=await fixture(t); await fs.writeFile(path.join(source,'a.png'),png); await album.sync();
  const previous=structuredClone(album.manifest);
  await fs.writeFile(path.join(source,'broken.jpg'),'not an image');
  await assert.rejects(album.sync()); assert.deepEqual(album.manifest,previous);
  await fs.unlink(path.join(source,'broken.jpg')); await fs.unlink(path.join(source,'a.png'));
  assert.equal(await album.sync(),false); assert.deepEqual(album.manifest,previous);
});
test('album replacement retires old cached files without touching source files',async t=>{
  const {album,source}=await fixture(t);await fs.writeFile(path.join(source,'a.png'),png);await album.sync();
  const old=album.manifest.photos[0].id;
  const changed=Buffer.concat([png,Buffer.from('a new content version')]);
  await fs.writeFile(path.join(source,'a.png'),changed);await album.sync();
  assert.notEqual(album.manifest.photos[0].id,old);
  await assert.rejects(fs.access(path.join(album.data,'photos',old)));
  assert.deepEqual(await fs.readFile(path.join(source,'a.png')),changed);
});
test('restart with a missing source still serves the saved manifest and cache',async t=>{
  const {album,source}=await fixture(t);await fs.writeFile(path.join(source,'a.png'),png);await album.sync();
  const next=new Album(path.join(source,'not-mounted'),album.data);await next.load();await next.prune();
  await assert.rejects(next.sync());assert.deepEqual(next.manifest,album.manifest);
  assert.deepEqual(await fs.readFile(path.join(next.data,'photos',next.manifest.photos[0].id)),png);
});
test('reject oversized inputs and ignore temporary files',async t=>{
  const {album,source}=await fixture(t);
  await fs.writeFile(path.join(source,'copy.tmp'),png); assert.equal(await album.sync(),false);
  const large=await fs.open(path.join(source,'large.jpg'),'w'); await large.truncate(9*1024*1024); await large.close();
  await assert.rejects(album.sync()); assert.equal(album.manifest.photos.length,0);
});
test('API requires a key, rejects writes/traversal and serves only declared photos',async t=>{
  const {album,source}=await fixture(t); await fs.writeFile(path.join(source,'a.png'),png); await album.sync();
  const key='testing-access-key-long-enough'; const server=makeServer(album,key,()=>({stale:true}));
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  t.after(()=>new Promise(resolve=>server.close(resolve)));
  const root=`http://127.0.0.1:${server.address().port}`;
  assert.equal((await fetch(root+'/manifest')).status,401);
  const headers={Authorization:'Bearer '+key};
  assert.equal((await fetch(root+'/manifest',{method:'POST',headers})).status,405);
  assert.equal((await fetch(root+'/photos/..%2fweather.json',{headers})).status,404);
  assert.equal((await fetch(root+'/photos/'+'0'.repeat(64),{headers})).status,404);
  const manifest=await (await fetch(root+'/manifest',{headers})).json();
  const bytes=Buffer.from(await (await fetch(root+'/photos/'+manifest.photos[0].id,{headers})).arrayBuffer());
  assert.deepEqual(bytes,png);
});
test('Sydney local date selects today, never tomorrow at UTC midnight',()=>{
  const rows=[{datetime:'2026-11-06T23:00:00Z',temperature:21,templow:11},{datetime:'2026-11-07T23:00:00Z',temperature:25,templow:12}];
  assert.equal(selectForecast(rows,new Date('2026-11-07T10:00:00Z'),'Australia/Sydney').temperature,21);
  assert.equal(selectForecast(rows,new Date('2026-11-08T10:00:00Z'),'Australia/Sydney').temperature,25);
});
test('stale weather is labelled; yesterday high and low never appear as today',()=>{
  const old={fetchedAt:'2026-11-07T00:00:00Z',observedAt:'2026-11-07T00:00:00Z',forecastDay:'2026-11-07',high:20,low:10};
  const out=weatherView(old,new Date('2026-11-08T00:00:00Z'),'Australia/Sydney');
  assert.equal(out.stale,true);assert.equal(out.high,null);assert.equal(out.low,null);
});
test('HA adapter only requests weather, keeps same-day forecast on partial failure',async()=>{
  const calls=[]; const fetcher=async(url,options)=>{
    calls.push({url,options});
    if(options.method==='POST')throw Error('forecast offline');
    return {ok:true,json:async()=>({state:'rainy',last_updated:'2026-11-07T01:00:00Z',attributes:{temperature:18,temperature_unit:'°C'}})};
  };
  const result=await fetchWeather({haUrl:'http://mock-ha',haToken:'test-only',weatherEntity:'weather.forecast_home',timeZone:'Australia/Sydney'},
    {forecastDay:'2026-11-07',high:22,low:12},fetcher,new Date('2026-11-07T02:00:00Z'));
  assert.equal(result.high,22);assert.equal(calls.length,2);
  assert.equal(calls[1].url,'http://mock-ha/api/services/weather/get_forecasts?return_response');
  assert.deepEqual(JSON.parse(calls[1].options.body),{entity_id:'weather.forecast_home',type:'daily'});
});
test('HA forecast response is normalised; unavailable current data is rejected',async()=>{
  const config={haUrl:'http://mock',haToken:'test',weatherEntity:'weather.forecast_home',timeZone:'Australia/Sydney'};
  const fetcher=async(url)=>({ok:true,json:async()=>url.includes('/states/')?
    {state:'sunny',last_updated:'2026-11-07T02:00:00Z',attributes:{temperature:19}}:
    {service_response:{'weather.forecast_home':{forecast:[{datetime:'2026-11-07T00:00:00Z',temperature:24,templow:14}]}}}});
  const result=await fetchWeather(config,{},fetcher,new Date('2026-11-07T02:00:00Z'));
  assert.equal(result.high,24); assert.equal(result.low,14);
  await assert.rejects(fetchWeather(config,{},async()=>({ok:true,json:async()=>({state:'unavailable',attributes:{}})})));
});

test('accept a 147-photo album and the 250-photo boundary; reject 251 without replacing the manifest',async t=>{
  const {album,source}=await fixture(t);
  const put=async i=>fs.writeFile(path.join(source,String(i).padStart(3,'0')+'.png'),Buffer.concat([png,Buffer.from(String(i))]));
  for(let i=0;i<147;i++)await put(i);
  await album.sync();assert.equal(album.manifest.photos.length,147);
  for(let i=147;i<250;i++)await put(i);
  await album.sync();assert.equal(album.manifest.photos.length,250);
  const previous=structuredClone(album.manifest);await put(250);
  await assert.rejects(album.sync(),/photo count limit/);
  assert.deepEqual(album.manifest,previous);
});
