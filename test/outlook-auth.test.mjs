import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, readFile, rm, stat, writeFile } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { outlookTokenProvider, saveRefreshToken, oauthRequest } from '../server/outlook-auth.mjs';
import { connect } from '../tools/connect-outlook-calendar.mjs';
const clientId='00000000-0000-0000-0000-000000000000';
const tokens={access_token:'synthetic-access',refresh_token:'synthetic-rotated',expires_in:3600,token_type:'Bearer'};
const json=(value,status=200)=>new Response(JSON.stringify(value),{status});
async function temp(t) {
  const dir=await mkdtemp(path.join(os.tmpdir(),'ambient-auth-'));t.after(()=>rm(dir,{recursive:true,force:true}));return path.join(dir,'synthetic-token.json');
}
test('refresh rotation persists privately, concurrent callers coalesce and access tokens stay in memory',async t=>{
  const tokenFile=await temp(t);await saveRefreshToken(tokenFile,clientId,'synthetic-initial');let calls=0;
  const get=outlookTokenProvider({clientId,tokenFile},async(url,options)=>{
    calls++;assert.equal(url,'https://login.microsoftonline.com/consumers/oauth2/v2.0/token');
    assert.equal(options.redirect,'error');assert.equal(options.method,'POST');
    const form=new URLSearchParams(options.body);assert.equal(form.get('refresh_token'),'synthetic-initial');
    assert.equal(form.get('grant_type'),'refresh_token');assert.match(form.get('scope'),/Calendars.ReadBasic$/);
    return json(tokens);
  });
  assert.deepEqual(await Promise.all([get(),get(),get()]),Array(3).fill('synthetic-access'));
  assert.equal(await get(),'synthetic-access');assert.equal(calls,1);
  const saved=await readFile(tokenFile,'utf8');assert.equal(JSON.parse(saved).refreshToken,'synthetic-rotated');
  assert.ok(!saved.includes('synthetic-access'));
  if(process.platform!=='win32')assert.equal((await stat(tokenFile)).mode&0o077,0);
  const restarted=outlookTokenProvider({clientId,tokenFile},async(url,options)=>{
    assert.equal(new URLSearchParams(options.body).get('refresh_token'),'synthetic-rotated');return json(tokens);
  });assert.equal(await restarted(),'synthetic-access');
});
test('failed renewal, invalid response and wrong registration keep the existing refresh token',async t=>{
  const tokenFile=await temp(t);await saveRefreshToken(tokenFile,clientId,'synthetic-initial');
  for(const fetcher of [async()=>json({error:'invalid_grant',error_description:'private'},400),
    async()=>json({...tokens,refresh_token:''}),async()=>new Response('x'.repeat(65537)),async()=>json(null)]) {
    const get=outlookTokenProvider({clientId,tokenFile},fetcher);await assert.rejects(get());
    assert.equal(JSON.parse(await readFile(tokenFile,'utf8')).refreshToken,'synthetic-initial');
  }
  await writeFile(tokenFile,JSON.stringify({clientId:'other',refreshToken:'synthetic'}),{mode:0o600});
  await assert.rejects(outlookTokenProvider({clientId,tokenFile},async()=>assert.fail('wrong client used'))());
  await assert.rejects(oauthRequest('arbitrary',{},async()=>assert.fail('unexpected route')));
});
test('device setup handles pending and slowdown without disclosing tokens or changing events',async t=>{
  const file=await temp(t);const output=[],delays=[];let calls=0;
  await connect({clientId,file},{log:value=>output.push(value),wait:async ms=>delays.push(ms),fetcher:async(url,options)=>{
    assert.equal(options.method,'POST');assert.equal(options.redirect,'error');calls++;
    if(url.endsWith('/devicecode'))return json({device_code:'private-device',user_code:'SYNTHETIC',expires_in:900,interval:5});
    if(calls===2)return json({error:'authorization_pending'},400);
    if(calls===3)return json({error:'slow_down'},400);
    return json(tokens);
  }});
  assert.deepEqual(delays,[5000,5000,10000]);assert.match(output.join(' '),/SYNTHETIC/);
  assert.ok(!output.join(' ').includes('private-device'));assert.ok(!output.join(' ').includes('synthetic-access'));
  assert.equal(JSON.parse(await readFile(file,'utf8')).refreshToken,'synthetic-rotated');
});
test('device setup decline, expiry and storage failure stop cleanly',async t=>{
  const file=await temp(t);
  const device={device_code:'synthetic-device',user_code:'SYNTHETIC',expires_in:5,interval:5};let clock=0;
  await assert.rejects(connect({clientId,file},{log:()=>{},now:()=>clock,wait:async ms=>clock+=ms,
    fetcher:async url=>{assert.ok(url.endsWith('/devicecode'));return json(device);}}));
  await assert.rejects(connect({clientId,file},{log:()=>{},wait:async()=>{},fetcher:async url=>url.endsWith('/devicecode')
    ?json({...device,expires_in:900}):json({error:'authorization_declined'},400)}));
  await assert.rejects(connect({clientId,file},{log:()=>{},wait:async()=>{},save:async()=>{throw Error('disk full');},
    fetcher:async url=>json(url.endsWith('/devicecode')?{...device,expires_in:900}:tokens)}));
});
