import { createServer } from 'node:http';
import { promises as fs } from 'node:fs';
import path from 'node:path';
import { createHash, timingSafeEqual } from 'node:crypto';
import { fileURLToPath } from 'node:url';

export const LIMITS = { count: 250, photoBytes: 8 * 1024 * 1024, albumBytes: 256 * 1024 * 1024 };
export const localDay = (date, timeZone) => new Intl.DateTimeFormat('en-CA', {
  timeZone, year: 'numeric', month: '2-digit', day: '2-digit'
}).format(new Date(date));
export async function atomicJson(file, value) {
  await fs.mkdir(path.dirname(file), { recursive: true });
  await fs.writeFile(file + '.tmp', JSON.stringify(value));
  await fs.rename(file + '.tmp', file);
}
async function readJson(file, fallback) {
  try { return JSON.parse(await fs.readFile(file, 'utf8')); } catch { return fallback; }
}
export function photoType(bytes) {
  if (bytes[0] === 255 && bytes[1] === 216 && bytes[2] === 255) return 'image/jpeg';
  if (bytes.subarray(0, 8).equals(Buffer.from([137,80,78,71,13,10,26,10]))) return 'image/png';
  if (bytes.toString('ascii',0,4) === 'RIFF' && bytes.toString('ascii',8,12) === 'WEBP') return 'image/webp';
  return null;
}
export class Album {
  constructor(source, data) { this.source = source; this.data = data; this.manifest = { version: 1, photos: [] }; }
  async load() { this.manifest = await readJson(path.join(this.data, 'manifest.json'), this.manifest); }
  async sync() {
    await this.prune();
    const entries = await fs.readdir(this.source, { withFileTypes: true });
    const files = entries.filter(e => e.isFile() && /\.(jpe?g|png|webp)$/i.test(e.name)).sort((a,b) => a.name.localeCompare(b.name));
    if (!files.length) return false; // An unmounted/empty source must not erase a working slideshow.
    if (files.length > LIMITS.count) throw Error('Album exceeds photo count limit');
    const photos = [];
    let total = 0;
    await fs.mkdir(path.join(this.data, 'photos'), { recursive: true });
    for (const file of files) {
      const source = path.join(this.source, file.name);
      const stat = await fs.lstat(source);
      if (!stat.isFile() || stat.isSymbolicLink() || !stat.size || stat.size > LIMITS.photoBytes) throw Error('Invalid photo size/type');
      const bytes = await fs.readFile(source);
      if (bytes.length !== stat.size || !photoType(bytes)) throw Error('Incomplete or unsupported photo');
      total += bytes.length;
      if (total > LIMITS.albumBytes) throw Error('Album exceeds cache budget');
      const id = createHash('sha256').update(bytes).digest('hex');
      if (photos.some(p => p.id === id)) continue;
      await fs.writeFile(path.join(this.data, 'photos', id), bytes);
      photos.push({ id, bytes: bytes.length, type: photoType(bytes) });
    }
    const manifest = { version: 1, photos };
    await atomicJson(path.join(this.data, 'manifest.json'), manifest);
    this.manifest = manifest;
    // In-flight clients can retry a newer manifest; their local last-good cache stays intact.
    await this.prune();
    return true;
  }
  async prune() {
    const keep = new Set(this.manifest.photos.map(p => p.id));
    const dir = path.join(this.data, 'photos');
    for (const name of await fs.readdir(dir).catch(() => [])) {
      if (/^[a-f0-9]{64}$/.test(name) && !keep.has(name)) await fs.unlink(path.join(dir, name));
    }
  }
}

export function calendarDay(day, offset) {
  const date = new Date(day + 'T12:00:00Z');
  date.setUTCDate(date.getUTCDate() + offset);
  return date.toISOString().slice(0, 10);
}
export function selectForecast(rows, now, zone, day = localDay(now, zone)) {
  if (!Array.isArray(rows)) return undefined;
  return rows.find(r => {
    const stamp = r?.datetime;
    if (typeof stamp !== 'string') return false;
    const calendar = stamp.slice(0, 10);
    const calendarStamp = Date.parse(calendar + 'T00:00:00Z');
    if (!Number.isFinite(calendarStamp) || new Date(calendarStamp).toISOString().slice(0, 10) !== calendar) return false;
    // BOM publishes local wall time without an offset. Its date already belongs
    // to the configured HA timezone; never interpret it in the Node host's zone.
    if (/^\d{4}-\d{2}-\d{2}(?:T\d{2}:\d{2}:\d{2}(?:\.\d+)?)?$/.test(stamp))
      return Number.isFinite(Date.parse(stamp + (stamp.length === 10 ? 'T00:00:00Z' : 'Z'))) &&
        (stamp.length === 10 || Number(stamp.slice(11, 13)) < 24) && calendar === day;
    return /(?:Z|[+-]\d{2}:\d{2})$/i.test(stamp) && Number.isFinite(Date.parse(stamp)) && localDay(stamp, zone) === day;
  });
}
const conditions = new Set(['clear-night','cloudy','fog','hail','lightning','lightning-rainy','partlycloudy','pouring','rainy','snowy','snowy-rainy','sunny','windy','windy-variant','exceptional']);
const numberOrNull = value => Number.isFinite(value) ? value : null;
function checkedAge(stamp, now, limit) {
  const age = now - Date.parse(stamp);
  return !Number.isFinite(age) || age < 0 || age > limit;
}
export function weatherView(cache, now, zone) {
  const today = localDay(now, zone);
  const days = [today, calendarDay(today, 1)];
  const legacy = cache?.forecastDay === today && (Number.isFinite(cache.high) || Number.isFinite(cache.low))
    ? [{ date: today, condition: null, high: numberOrNull(cache.high), low: numberOrNull(cache.low), checkedAt: cache.forecastAt ?? null, cached: true }] : [];
  const dailyForecasts = (Array.isArray(cache?.dailyForecasts) ? cache.dailyForecasts : legacy)
    .filter(row => row && days.includes(row.date)).slice(0, 2)
    .map(row => ({ ...row, stale: row.cached === true || checkedAge(row.checkedAt, now, 21600000) }));
  return { ...cache, timeZone: zone,
    dailyForecasts,
    high: cache?.forecastDay === today ? cache.high : null,
    low: cache?.forecastDay === today ? cache.low : null,
    stale: checkedAge(cache?.fetchedAt, now, 1800000) || checkedAge(cache?.haReportedAt ?? cache?.observedAt, now, 10800000),
    forecastStale: checkedAge(cache?.forecastAt, now, 21600000) || dailyForecasts.some(row => row.stale) };
}
export async function fetchWeather({ haUrl, haToken, weatherEntity, timeZone }, previous, fetcher = fetch, now = new Date()) {
  if (!/^weather\.[a-z0-9_]+$/.test(weatherEntity)) throw Error('Invalid weather entity');
  const call = async (route, body) => {
    const response = await fetcher(haUrl.replace(/\/$/, '') + route, {
      method: body ? 'POST' : 'GET', redirect: 'error', signal: AbortSignal.timeout(10000),
      headers: { Authorization: 'Bearer ' + haToken, 'Content-Type': 'application/json' },
      ...(body ? { body: JSON.stringify(body) } : {})
    });
    if (!response.ok) throw Error('Weather request failed');
    return response.json();
  };
  // Only these two fixed operations exist; no device-control or arbitrary proxy route.
  const state = await call('/api/states/' + weatherEntity);
  if (!Number.isFinite(state.attributes?.temperature) || ['unknown','unavailable'].includes(state.state)) throw Error('Weather unavailable');
  let rows;
  try {
    const result = await call('/api/services/weather/get_forecasts?return_response', { entity_id: weatherEntity, type: 'daily' });
    rows = result.service_response?.[weatherEntity]?.forecast;
  } catch { /* Keep today's previous forecast when only the forecast request fails. */ }
  const today = localDay(now, timeZone);
  const old = previous?.weatherEntity === weatherEntity ? previous : {};
  const dailyForecasts = [today, calendarDay(today, 1)].map(date => {
    const retained = (Array.isArray(old.dailyForecasts) ? old.dailyForecasts.find(r => r?.date === date) : null)
      ?? (old.forecastDay === date ? { low: old.low, checkedAt: old.forecastAt } : null);
    const usable = Array.isArray(rows) ? rows.filter(row => row &&
      (conditions.has(row.condition) || Number.isFinite(row.temperature) || Number.isFinite(row.templow))) : [];
    const row = selectForecast(usable, now, timeZone, date);
    const condition = conditions.has(row?.condition) ? row.condition : null;
    const high = numberOrNull(row?.temperature), suppliedLow = numberOrNull(row?.templow);
    if (condition || high !== null || suppliedLow !== null) {
      let low = suppliedLow, lowCheckedAt = low === null ? null : now.toISOString();
      // BOM may omit Day 0's minimum after morning. Keep only the last supplied
      // minimum for this provider/date/unit; tomorrow's value never fills today.
      const priorCheck = retained?.lowCheckedAt ?? retained?.checkedAt;
      const priorAge = now - Date.parse(priorCheck);
      if (low === null && Number.isFinite(retained?.low) && Number.isFinite(priorAge) && priorAge >= 0
          && (old.unit === undefined || old.unit === (state.attributes.temperature_unit ?? '°C'))) {
        low = retained.low; lowCheckedAt = priorCheck;
      }
      // checkedAt is this successful row check. Preserve the low's source-check
      // time separately without an extra TV label or a new stale-data warning.
      return { date, condition, high, low, lowCheckedAt, checkedAt: now.toISOString(), cached: false };
    }
    const previousRow = Array.isArray(old.dailyForecasts) ? old.dailyForecasts.find(r => r?.date === date) : null;
    if (previousRow) return { ...previousRow, cached: true };
    // Legacy cache compatibility: never promote an old entity condition into a forecast.
    if (old.forecastDay === date && (Number.isFinite(old.high) || Number.isFinite(old.low)))
      return { date, condition: null, high: numberOrNull(old.high), low: numberOrNull(old.low), checkedAt: old.forecastAt ?? null, cached: true };
    return null;
  }).filter(Boolean);
  const forecast = dailyForecasts.find(row => row.date === today);
  const conditionSource = state.attributes.condition_source ?? 'unspecified';
  return { weatherEntity, temperature: state.attributes.temperature, unit: state.attributes.temperature_unit ?? '°C',
    // HA's report time proves entity reporting, not the upstream observation age.
    condition: state.state, conditionSource,
    // Only an explicit observation provenance may label a condition as observed.
    currentCondition: conditionSource === 'observation' && conditions.has(state.state) ? state.state : null,
    haReportedAt: state.last_reported ?? state.last_updated,
    observedAt: state.last_reported ?? state.last_updated, fetchedAt: now.toISOString(),
    dailyForecasts, forecastDay: today,
    high: forecast?.high ?? null, low: forecast?.low ?? null,
    forecastAt: forecast?.checkedAt ?? null,
    attribution: state.attributes.attribution ?? '' };
}
function authorised(header, key) {
  const a = Buffer.from(header ?? ''), b = Buffer.from('Bearer ' + key);
  return a.length === b.length && timingSafeEqual(a,b);
}
export function makeServer(album, key, weather) {
  return createServer(async (req, res) => {
    const json = (status, body) => { res.writeHead(status, { 'Content-Type':'application/json', 'Cache-Control':'no-store', 'X-Content-Type-Options':'nosniff' }); res.end(JSON.stringify(body)); };
    if (req.method !== 'GET') return json(405, { error: 'Read only' });
    if (req.url === '/health') return json(200, { ok:true });
    if (!authorised(req.headers.authorization, key)) return json(401, { error:'Unauthorised' });
    if (req.url === '/manifest') return json(200, album.manifest);
    if (req.url === '/weather') return json(200, weather());
    const match = /^\/photos\/([a-f0-9]{64})$/.exec(req.url);
    if (!match || !album.manifest.photos.some(p => p.id === match[1])) return json(404, { error:'Not found' });
    try {
      const bytes = await fs.readFile(path.join(album.data, 'photos', match[1]));
      res.writeHead(200, { 'Content-Type':photoType(bytes), 'Cache-Control':'private, max-age=300', 'X-Content-Type-Options':'nosniff' });
      res.end(bytes);
    } catch { json(503, { error:'Photo temporarily unavailable' }); }
  });
}
export async function main() {
  const env = process.env;
  // HA add-on options are read only by this installed application, never logged.
  const options = await readJson('/data/options.json', {});
  const key = env.AMBIENT_KEY || options.access_key;
  if (!key || key.length < 24) throw Error('Configure an ambient access key of at least 24 characters');
  const data = env.DATA_DIR || './data';
  const album = new Album(env.PHOTO_DIR || '/share/bravia-ambient', data);
  await album.load(); await album.prune();
  let cached = await readJson(path.join(data, 'weather.json'), {});
  const config = { haUrl: env.HA_URL || 'http://supervisor/core', haToken:env.HA_TOKEN || env.SUPERVISOR_TOKEN,
    weatherEntity:env.WEATHER_ENTITY || options.weather_entity || 'weather.forecast_home',
    timeZone:env.TIME_ZONE || options.time_zone || 'Australia/Sydney' };
  localDay(new Date(), config.timeZone); // Reject invalid configuration before serving requests.
  // A provider change must not relabel another provider's last-good reading.
  if (cached.weatherEntity !== config.weatherEntity) cached = {};
  let busy = false;
  const update = async () => {
    if (busy) return;
    busy = true;
    try { await album.sync(); } catch { console.warn('Photo sync failed; keeping previous album'); }
    if (config.haToken) {
      try { const next = await fetchWeather(config,cached); await atomicJson(path.join(data,'weather.json'),next); cached = next; }
      catch { console.warn('Weather update failed; keeping previous reading'); }
    }
    busy = false;
  };
  await update();
  const timer = setInterval(update, 300000);
  const server = makeServer(album,key,()=>weatherView(cached,new Date(),config.timeZone));
  server.listen(Number(env.PORT || 8099), env.BIND_HOST || '127.0.0.1', ()=>console.log('Ambient server ready'));
  const stop = () => { clearInterval(timer); server.close(()=>process.exit(0)); };
  process.on('SIGINT',stop); process.on('SIGTERM',stop);
  return server;
}
if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) main().catch(()=>{ console.error('Ambient server startup failed; check configuration'); process.exitCode=1; });
