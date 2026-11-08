import { test } from 'node:test';
import assert from 'node:assert/strict';
import { calendarDay, selectForecast, fetchWeather, weatherView } from '../server/server.mjs';

const config = {haUrl:'http://synthetic-ha',haToken:'synthetic-only',weatherEntity:'weather.example',timeZone:'Australia/Sydney'};
const now = new Date('2026-11-08T02:00:00Z');
const state = {state:'rainy',last_reported:now.toISOString(),attributes:{temperature:17,condition_source:'daily'}};
const rows = [
  {datetime:'2026-11-08T00:00:00',condition:'rainy',temperature:19,templow:11},
  {datetime:'2026-11-09T00:00:00',condition:'partlycloudy',temperature:21,templow:14}
];
const get = (forecast=rows, current=state, fail=false) => async url => {
  if(url.includes('/states/'))return {ok:true,json:async()=>current};
  if(fail)throw Error('synthetic outage');
  return {ok:true,json:async()=>({service_response:{[config.weatherEntity]:{forecast}}})};
};
test('observed temperature and separate daily forecasts retain condition provenance',async()=>{
  const result=await fetchWeather(config,{},get(),now);
  assert.equal(result.temperature,17);
  assert.equal(result.conditionSource,'daily');
  assert.equal(result.currentCondition,null);
  assert.deepEqual(result.dailyForecasts.map(r=>[r.date,r.condition,r.high,r.low]),[
    ['2026-11-08','rainy',19,11],['2026-11-09','partlycloudy',21,14]]);
  assert.equal(result.high,19);assert.equal(result.low,11); // old APK compatibility
});
test('failed forecast does not refresh retained age, even with new temperature',async()=>{
  const previous=await fetchWeather(config,{},get(),now);
  const later=new Date(now.getTime()+300000);
  const result=await fetchWeather(config,previous,get([],state,true),later);
  assert.equal(result.fetchedAt,later.toISOString());
  assert.equal(result.dailyForecasts[1].checkedAt,now.toISOString());
  assert.equal(result.dailyForecasts[1].cached,true);
  assert.equal(weatherView(result,later,config.timeZone).forecastStale,true);
});
test('missing today never substitutes tomorrow; partial valid rows keep missing values explicit',async()=>{
  const result=await fetchWeather(config,{},get([rows[1]]),now);
  assert.equal(result.high,null);
  assert.equal(result.dailyForecasts.length,1);
  const partial=await fetchWeather(config,{},get([{...rows[0],temperature:null,condition:'invented'}]),now);
  assert.equal(partial.dailyForecasts[0].condition,null);
  assert.equal(partial.dailyForecasts[0].high,null);
  assert.equal(partial.dailyForecasts[0].low,11);
});
test('provider changes discard forecasts and malformed records do not hide valid same-day rows',async()=>{
  const previous=await fetchWeather(config,{},get(),now);
  previous.weatherEntity='weather.other';
  assert.equal((await fetchWeather(config,previous,get([],state,true),now)).dailyForecasts.length,0);
  const result=await fetchWeather(config,{},get([null,{datetime:'bad',temperature:99},{datetime:rows[0].datetime},...rows]),now);
  assert.equal(result.high,19);
});
test('midnight promotes only the correctly dated retained record, preserving its original age',async()=>{
  const previous=await fetchWeather(config,{},get(),now);
  const midnight=new Date('2026-11-08T13:00:00Z');
  const result=await fetchWeather(config,previous,get([],state,true),midnight);
  assert.deepEqual(result.dailyForecasts.map(r=>r.date),['2026-11-09']);
  assert.equal(result.high,21);
  assert.equal(result.forecastAt,now.toISOString());
  assert.equal(weatherView(previous,midnight,config.timeZone).high,null);
});
test('calendar increment handles DST, month and year boundaries',()=>{
  for(const [date,next] of [['2026-10-04','2026-10-05'],['2026-04-05','2026-04-06'],['2026-12-31','2027-01-01'],['2028-02-29','2028-03-01']])
    assert.equal(calendarDay(date,1),next);
});
test('future and unknown check timestamps cannot look fresh',()=>{
  const future='2026-11-08T03:00:00Z';
  for(const stamp of [future,null,'bad']) {
    const out=weatherView({fetchedAt:stamp,observedAt:stamp,forecastAt:stamp},now,config.timeZone);
    assert.equal(out.stale,true);assert.equal(out.forecastStale,true);
  }
});
test('legacy cache retains only same-provider, same-date range without inventing condition',async()=>{
  const old={weatherEntity:config.weatherEntity,forecastDay:'2026-11-08',high:19,low:11,forecastAt:now.toISOString()};
  const result=await fetchWeather(config,old,get([],state,true),now);
  assert.equal(result.high,19);assert.equal(result.dailyForecasts[0].condition,null);
  assert.equal(result.dailyForecasts[0].cached,true);
  assert.equal(weatherView(old,now,config.timeZone).dailyForecasts[0].high,19);
  assert.equal(weatherView(old,now,config.timeZone).dailyForecasts[0].condition,null);
});
test('impossible calendar dates and local 24-hour rollover cannot be treated as the requested day',()=>{
  const march=new Date('2026-03-02T02:00:00Z');
  assert.equal(selectForecast([{datetime:'2026-02-30T00:00:00+11:00',temperature:99}],march,config.timeZone),undefined);
  assert.equal(selectForecast([{datetime:'2026-11-08T24:00:00',temperature:99}],now,config.timeZone),undefined);
});
