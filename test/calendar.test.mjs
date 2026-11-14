import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, readFile, rm } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { CalendarFeed, fetchCalendar, calendarView, calendarSource, restoreCalendar, dayAt, daysUntil, validDay } from '../server/calendar.mjs';
import { makeServer, atomicJson } from '../server/server.mjs';

const now = new Date('2032-12-30T13:00:00Z'); // Sydney Dec 31, entirely synthetic.
const config = { outlookClientId: 'synthetic-client', calendarId: 'synthetic-calendar', getAccessToken: async () => 'synthetic-only' };
const event = (summary = 'Example birthday', date = '2033-01-01', extra = {}) => ({ summary,
  start: { date }, end: { date: new Date(Date.parse(date) + 86400000).toISOString().slice(0, 10) },
  uid: summary, description: 'private notes\nambient:birth-year=2000', location: 'private location', ...extra });
const graphEvent = event => ({ id: event.uid, subject: event.summary, isAllDay: true,
  isCancelled: event.status === 'cancelled',
  start: { dateTime: event.start?.date ? event.start.date + 'T00:00:00.0000000' : event.start?.dateTime, timeZone: 'AUS Eastern Standard Time' },
  end: { dateTime: event.end?.date + 'T00:00:00.0000000', timeZone: 'AUS Eastern Standard Time' },
  body: { contentType: 'text', content: event.description } });
const provider = events => async () => new Response(JSON.stringify({ value: Array.isArray(events) ? events.map(graphEvent) : events }));

test('narrow read-only Graph GET never requests descriptions or derives title metadata', async () => {
  const calls = [];
  const saved = await fetchCalendar(config, async (url, options) => {
    calls.push(url); assert.equal(options.method, 'GET'); assert.equal(options.redirect, 'error');
    assert.equal(options.headers.Authorization, 'Bearer synthetic-only');
    return provider([event()])(url);
  }, now);
  assert.equal(calls.length, 1);
  const query = new URL(calls[0]); assert.equal(query.pathname, '/v1.0/me/calendars/synthetic-calendar/calendarView');
  assert.equal(query.searchParams.get('startDateTime'), '2032-12-30T00:00:00Z');
  assert.equal(query.searchParams.get('endDateTime'), '2034-02-05T00:00:00Z');
  const view = calendarView(saved, now);
  assert.equal(view.occurrences[0].daysUntil, 1);
  assert.deepEqual(Object.keys(view.occurrences[0]).sort(), ['occurrenceId', 'title', 'localDate', 'endDateExclusive', 'daysUntil'].sort());
  assert.ok(!JSON.stringify(view).includes('private')); assert.ok(!JSON.stringify(view).includes('2000'));
  assert.equal(query.searchParams.get('$select'),'id,subject,isAllDay,start,end,isCancelled');
  assert.equal(view.schemaVersion,2);

});

test('Sydney midnight, UTC/DST boundaries, leap dates and year rollover use civil days', () => {
  for (const [instant, day] of [['2032-12-31T12:59:59Z','2032-12-31'], ['2032-12-31T13:00:00Z','2033-01-01'],
    ['2033-10-01T13:59:59Z','2033-10-01'],['2033-10-01T14:00:00Z','2033-10-02'],
    ['2033-04-02T13:00:00Z','2033-04-03']]) assert.equal(dayAt(new Date(instant)), day);
  assert.equal(daysUntil('2032-03-01','2032-02-28'), 2);
  assert.equal(validDay('2032-02-29'), true); assert.equal(validDay('2033-02-29'), false);
});

test('published subjects are exact, including publisher-owned milestones, nicknames and whitespace',async()=>{
  for(const subject of ['  Example’s 40th birthday  ', 'Example anniversary — 20 years',
    'Published nickname / 2033 @ home', 'Example holiday 1/2']) {
    const saved=await fetchCalendar(config,provider([event(subject)]),now);
    const view=calendarView(saved,now);
    assert.equal(view.occurrences[0].title,subject);
    assert.equal(JSON.parse(JSON.stringify(saved)).occurrences[0].title,subject);
    assert.equal(restoreCalendar(saved,calendarSource(config)).occurrences[0].title,subject);
    assert.ok(!Object.hasOwn(view.occurrences[0],'kind'));
    assert.ok(!Object.hasOwn(view.occurrences[0],'displayAge'));
  }
});

test('multi-day events survive startup and offline ageing until exclusive Sydney end',async()=>{
  // Starts before the query window; Graph's overlapping event must not be clipped by start date.
  const span=event('Published summer break','2032-12-01',{end:{date:'2033-01-03'}});
  const saved=await fetchCalendar(config,provider([span]),now);
  assert.equal(saved.occurrences[0].endDateExclusive,'2033-01-03');
  const feed=new CalendarFeed(config,JSON.parse(JSON.stringify(saved)),async()=>{},async()=>{throw Error('offline');});
  await assert.rejects(feed.update(now));
  for(const instant of ['2032-12-30T13:00:00Z','2033-01-01T13:00:00Z','2033-01-02T12:59:59Z']) {
    const view=feed.view(new Date(instant));assert.equal(view.occurrences.length,1);assert.equal(view.occurrences[0].daysUntil,0);
  }
  assert.equal(feed.view(new Date('2033-01-02T13:00:00Z')).occurrences.length,0);
  const dst=await fetchCalendar(config,provider([event('Published DST break','2033-10-01',{end:{date:'2033-10-04'}})]),new Date('2033-09-30T14:00:00Z'));
  assert.equal(calendarView(dst,new Date('2033-10-03T12:59:59Z')).occurrences.length,1);
  assert.equal(calendarView(dst,new Date('2033-10-03T13:00:00Z')).occurrences.length,0);
  assert.equal(restoreCalendar({...saved,schemaVersion:1},calendarSource(config)),null);
});

test('complete pagination, overlap deduplication and failed later pages never promote partial results',async()=>{
  let first;
  const saved=await fetchCalendar(config,async url=>{
    if(!first){first=url;return new Response(JSON.stringify({value:[graphEvent(event())], '@odata.nextLink':url+'&$skiptoken=next'}));}
    assert.equal(url,first+'&$skiptoken=next');
    return new Response(JSON.stringify({value:[graphEvent(event()),graphEvent(event('Second','2033-01-02'))]}));
  },now);
  assert.equal(saved.occurrences.length,2);
  for(const link of ['https://other.invalid/steal','http://graph.microsoft.com/steal',
    'https://graph.microsoft.com/v1.0/me/messages','',42]) {
    let calls=0;await assert.rejects(fetchCalendar(config,async()=>{
      calls++;return new Response(JSON.stringify({value:[], '@odata.nextLink':link}));
    },now));assert.equal(calls,1,'never send bearer to an unapproved URL');
  }
  await assert.rejects(fetchCalendar(config,async url=>new Response(JSON.stringify({value:[], '@odata.nextLink':url})),now));
  let calls=0;const feed=new CalendarFeed(config,saved,async()=>assert.fail('partial result persisted'),async url=>{
    if(calls++===0)return new Response(JSON.stringify({value:[], '@odata.nextLink':url+'&$skiptoken=next'}));
    return new Response('',{status:429});
  });
  await assert.rejects(feed.update(now));assert.deepEqual(feed.snapshot,saved);
});

test('reject unexpected Graph timezone, nonmidnight/timed dates, null pages and oversized sanitised snapshots',async()=>{
  for(const patch of [{start:{dateTime:'2033-01-01T00:00:00',timeZone:'UTC'}},
    {start:{dateTime:'2033-01-01T01:00:00',timeZone:'Australia/Sydney'}},
    {isAllDay:false},{isCancelled:undefined},{id:'x'.repeat(1025)}])
    await assert.rejects(fetchCalendar(config,async()=>new Response(JSON.stringify({value:[{...graphEvent(event()),...patch}]})),now));
  await assert.rejects(fetchCalendar(config,async()=>new Response('null'),now));
  const many=Array.from({length:180},(_,i)=>graphEvent(event('界'.repeat(160),'2033-01-01',{uid:'unique-'+i,description:''})));
  await assert.rejects(fetchCalendar(config,async url=>new Response(JSON.stringify({value:url.includes('$skiptoken')?many.slice(90):many.slice(0,90),
    ...(!url.includes('$skiptoken')?{'@odata.nextLink':url+'&$skiptoken=next'}:{})})),now),/snapshot/);
});

test('provider-expanded exceptions, edits/deletes, duplicate overlap and same-day occasions replace whole snapshot', async () => {
  let events = [event(), event(), event('Example occasion','2033-01-01'), event('Cancelled','2033-01-02',{ status: 'cancelled' })];
  const feed = new CalendarFeed(config, null, async () => {}, url => provider(events)(url));
  await feed.update(now); assert.equal(feed.snapshot.occurrences.length, 2);
  const id = feed.snapshot.occurrences.find(x => x.title === 'Example birthday').occurrenceId;
  events = [event('Edited birthday','2033-01-02', { uid: 'Example birthday', recurrence_id: '2033-01-01' })];
  await feed.update(now); assert.equal(feed.snapshot.occurrences.length, 1);
  assert.equal(feed.snapshot.occurrences[0].title, 'Edited birthday');
  assert.notEqual(feed.snapshot.occurrences[0].occurrenceId, id);
  events = []; await feed.update(now); assert.deepEqual(feed.view(now).occurrences, []);
  assert.equal(feed.view(now).stale, false);
});

test('invalid, unavailable, truncated and oversized upstreams fail instead of clearing calendar', async () => {
  for (const events of [null, {}, [event('', '2033-01-01')], [event('Timed','2033-01-01',{start:{dateTime:'2033-01-01T00:00:00Z'}})],
    [event('Invalid range','2033-01-01',{end:{date:'2033-01-01'}})], Array(257).fill(event()),
    [event(),event('Conflicting birthday','2033-01-01',{uid:'Example birthday'})]])
    await assert.rejects(fetchCalendar(config, provider(events), now));
  for (const result of [new Response('',{status:401}),new Response('['),new Response('x'.repeat(262145))])
    await assert.rejects(fetchCalendar(config,async()=>result,now));
  await assert.rejects(fetchCalendar({...config,calendarId:''},provider([]),now));
});

test('durable restart, failure retention, offline expiry, 24h stale and independent cache identity', async t => {
  const dir = await mkdtemp(path.join(os.tmpdir(),'ambient-calendar-')); t.after(()=>rm(dir,{recursive:true,force:true}));
  const file = path.join(dir,'calendar.json'); let fail=false;
  const feed=new CalendarFeed(config,null,next=>atomicJson(file,next),url=>{
    if(fail)throw Error('private error');return provider([event('Today','2032-12-31'),event()])(url);
  });
  assert.equal(feed.view(now).fetchedAt,null);await feed.update(now);fail=true;
  await assert.rejects(feed.update(now));assert.equal(feed.view(now).occurrences.length,2);
  const saved=JSON.parse(await readFile(file,'utf8'));
  const restarted=new CalendarFeed(config,saved,async()=>{},provider([]));
  assert.deepEqual(restarted.view(now),feed.view(now));
  assert.equal(restarted.view(new Date(+now+86399999)).stale,false);
  assert.equal(restarted.view(new Date(+now+86400000)).stale,true);
  assert.equal(restarted.view(new Date('2032-12-31T13:00:00Z')).occurrences.length,1);
  assert.equal(restarted.view(new Date('2033-01-01T13:00:00Z')).occurrences.length,0);
  assert.equal(restarted.view(new Date(+now-1)).stale,true);
  assert.equal(restoreCalendar(saved,calendarSource({...config,calendarId:'other'})),null);
  assert.equal(restoreCalendar({...saved,schemaVersion:1},calendarSource(config)),null);
  assert.equal(new CalendarFeed({...config,calendarId:''},saved,async()=>{}).view(now).fetchedAt,null);
});

test('poll serialization and failed disk writes preserve previous snapshot', async () => {
  const saved=await fetchCalendar(config,provider([event()]),now);
  let release, calls=0; const gate=new Promise(resolve=>release=resolve);
  const feed=new CalendarFeed(config,saved,async()=>{throw Error('disk full');},async url=>{
    calls++;await gate;return provider([])(url);
  });
  const pending=feed.update(now);await feed.update(now);assert.equal(calls,1);release();
  await assert.rejects(pending,/disk full/);assert.equal(feed.busy,false);
  assert.deepEqual(feed.snapshot,saved);
});

test('rolling horizon retains future next-year rows but exposes only next twelve months',async()=>{
  const saved=await fetchCalendar(config,provider([event('Beyond view','2034-01-20')]),now);
  assert.equal(saved.occurrences.length,1);assert.equal(calendarView(saved,now).occurrences.length,0);
  assert.equal(calendarView(saved,new Date('2033-02-01T00:00:00Z')).occurrences.length,1);
});

test('/calendar is authenticated, read only, no-store and independently unavailable',async t=>{
  const key='synthetic-calendar-key-only';const server=makeServer({manifest:{photos:[]}},key,()=>({temperature:20}));
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));t.after(()=>new Promise(resolve=>server.close(resolve)));
  const base=`http://127.0.0.1:${server.address().port}`,headers={Authorization:'Bearer '+key};
  assert.equal((await fetch(base+'/calendar')).status,401);
  assert.equal((await fetch(base+'/calendar',{method:'POST',headers})).status,405);
  const result=await fetch(base+'/calendar',{headers});assert.equal(result.headers.get('cache-control'),'no-store');
  assert.deepEqual(await result.json(),{schemaVersion:2,timezone:'Australia/Sydney',fetchedAt:null,stale:true,occurrences:[]});
  assert.equal((await (await fetch(base+'/weather',{headers})).json()).temperature,20);
});
