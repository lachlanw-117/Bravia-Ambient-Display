import test from 'node:test';
import assert from 'node:assert/strict';
import { NoticeFeed, fetchNotices, noticesView, sanitiseItems, restoreNotices, NOTICE_LIMITS, noticeFailureReason } from '../server/notices.mjs';
import { makeServer } from '../server/server.mjs';

const now = new Date('2026-11-14T06:00:00Z');
const at = now.getTime()/1000;
const config = { haUrl:'http://synthetic-ha', haToken:'synthetic-only' };
const row = (extra = {}) => ({ key:'synthetic_fault', occurrence_id:'synthetic:1', title:'A household device needs attention',
  category:'devices', kind:'fault', severity:'warning', active:true, first_detected:at-30,
  last_valid_observation:at, resolved_at:null, quality:'valid', display_until:0,
  evidence:[{entity_id:'sensor.private',state:'private'}], phone_routes:['private-recipient'], ...extra });
const response = items => new Response(JSON.stringify({state:'ready',attributes:{schema_version:1,items}}));

test('diagnostics contain only fixed labels, never upstream exception content',()=>{
  assert.equal(noticeFailureReason(Error('Invalid notice timestamp')),'Invalid notice timestamp');
  assert.equal(noticeFailureReason(Error('synthetic private upstream body')),'Request or storage failure');
});

test('adapter reads only the TV view and allowlists public fields', async () => {
  let calls=0;
  const result=await fetchNotices(config,async (url,options) => {
    calls++;assert.equal(url,'http://synthetic-ha/api/states/sensor.household_attention_tv');
    assert.equal(options.method,'GET');assert.equal(options.redirect,'error');
    assert.equal(options.headers.Authorization,'Bearer synthetic-only');
    return response([row()]);
  },now);
  assert.equal(calls,1);assert.equal(result.fetchedAt,now.toISOString());
  assert.deepEqual(Object.keys(result.items[0]).sort(),['occurrenceId','category','kind','severity','title','active',
    'firstDetected','lastValidObservation','resolvedAt','expiresAt','quality'].sort());
  assert.ok(!JSON.stringify(result).includes('private'));
});

test('grouped upgrades count distinct last-confirmed components with uncertainty', () => {
  const item=sanitiseItems([row({key:'upgrades_available',kind:'attention',quality:'unknown',details:[
    {entity_id:'update.synthetic_a',latest_version:'2'},{entity_id:'update.synthetic_a',latest_version:'2'},
    {entity_id:'update.synthetic_b',latest_version:'3'}]})])[0];
  assert.equal(item.updateCount,2);assert.equal(item.title,'2 updates available · confirmation incomplete');
  assert.ok(!JSON.stringify(item).includes('entity_id'));
  const saved={version:1,fetchedAt:now.toISOString(),items:[item]};
  assert.deepEqual(restoreNotices(JSON.parse(JSON.stringify(saved))),saved);
  assert.equal(sanitiseItems([row({key:'upgrades_available',kind:'attention',quality:'unknown',details:[]})])[0].title,
    'Update status uncertain');
});

test('unavailable/schema/malformed/oversized feeds fail rather than clear notices',async () => {
  for (const state of [null,{}, {state:'unavailable'}, {state:'ready',attributes:{schema_version:2,items:[]}},
    {state:'ready',attributes:{schema_version:1,items:null}},
    {state:'ready',attributes:{schema_version:1,items:[row({title:''})]}},
    {state:'ready',attributes:{schema_version:1,items:[row(),row()]}}]) {
    await assert.rejects(fetchNotices(config,async()=>new Response(JSON.stringify(state)),now));
  }
  await assert.rejects(fetchNotices(config,async()=>new Response('no',{status:401}),now));
  await assert.rejects(fetchNotices(config,async()=>new Response('x'.repeat(NOTICE_LIMITS.bytes+1)),now),/too large/);
  for (const extra of [{first_detected:NaN},{kind:'other'},{active:'true'},{quality:'other'},
    {title:'bad\ntext'},{severity:'critical'},{kind:'event',display_until:0}])
    assert.throws(()=>sanitiseItems([row(extra)]));
  assert.throws(()=>sanitiseItems(Array(65).fill(row())));
  const multibyte=Array.from({length:64},(_,i)=>row({occurrence_id:'界'.repeat(250)+i,title:'界'.repeat(300),category:'界'.repeat(64)}));
  await assert.rejects(fetchNotices(config,async()=>response(multibyte),now),/Sanitised notices too large/);
});

test('faults persist during outage; timed events expire; a successful empty feed replaces cache',async () => {
  let saved,fail=false,items=[row(),row({occurrence_id:'wash:1',kind:'event',active:false,display_until:at+60})];
  const fetcher=async()=>{if(fail)throw Error('offline');return response(items);};
  const feed=new NoticeFeed(config,null,async value=>{saved=JSON.parse(JSON.stringify(value));},fetcher);
  assert.equal(feed.view(now).fetchedAt,null);
  await feed.update(now);assert.equal(feed.view(now).items.length,2);
  fail=true;await assert.rejects(feed.update(new Date(now.getTime()+15000)));
  const later=new Date(now.getTime()+120000);
  assert.equal(feed.view(later).stale,true);assert.equal(feed.view(later).items.length,1);
  assert.equal(feed.view(new Date(now.getTime()+119999)).stale,false);
  const restarted=new NoticeFeed(config,saved,async value=>{saved=value;},fetcher);
  assert.deepEqual(restarted.view(later),feed.view(later));
  fail=false;items=[];await restarted.update(later);
  assert.deepEqual(restarted.view(later).items,[]);assert.equal(restarted.view(later).stale,false);
  assert.deepEqual(saved.items,[]);
});

test('persist failures retain the previous snapshot; overlapping polls do not race',async () => {
  let release,calls=0;
  const gate=new Promise(resolve=>{release=resolve;});
  const saved=await fetchNotices(config,async()=>response([row()]),now);
  const feed=new NoticeFeed(config,saved,async()=>{throw Error('disk full');},async()=>{calls++;await gate;return response([]);});
  const pending=feed.update(now);await feed.update(now);assert.equal(calls,1);release();
  await assert.rejects(pending,/disk full/);assert.equal(feed.view(now).items.length,1);
  assert.equal(feed.busy,false);assert.equal(restoreNotices({version:2}),null);
  assert.equal(noticesView(null,now).stale,true);
});

test('routed snapshot removes paused/recovered items; event expiry is independent of active flag',async () => {
  let items=[row()];const feed=new NoticeFeed(config,null,async()=>{},async()=>response(items));
  await feed.update(now);items=[];await feed.update(now);assert.deepEqual(feed.view(now).items,[]);
  items=[row({kind:'event',display_until:at,active:true})];await feed.update(now);
  assert.deepEqual(feed.view(now).items,[]);
});

test('/notices requires the host key, rejects writes and exposes unavailable status before first fetch',async t => {
  const key='synthetic-notice-access-key-only';
  const server=makeServer({manifest:{version:1,photos:[]}},key,()=>({}));
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  t.after(()=>new Promise(resolve=>server.close(resolve)));
  const url=`http://127.0.0.1:${server.address().port}/notices`;
  assert.equal((await fetch(url)).status,401);
  assert.equal((await fetch(url,{headers:{Authorization:'Bearer wrong'}})).status,401);
  const headers={Authorization:'Bearer '+key};
  assert.equal((await fetch(url,{method:'POST',headers})).status,405);
  const result=await fetch(url,{headers});assert.equal(result.headers.get('cache-control'),'no-store');
  assert.deepEqual(await result.json(),{version:1,fetchedAt:null,stale:true,items:[]});
});
