import { createHash } from 'node:crypto';

export const ZONE = 'Australia/Sydney';
export const CALENDAR_LIMITS = { bytes: 262144, items: 256, snapshotBytes: 60000 };
export const dayAt = now => new Intl.DateTimeFormat('en-CA', {
  timeZone: ZONE, year: 'numeric', month: '2-digit', day: '2-digit'
}).format(now);
export function validDay(value) {
  return typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value) &&
    Number.isFinite(Date.parse(value)) && new Date(value).toISOString().slice(0, 10) === value;
}
export const addDays = (day, count) => new Date(Date.parse(day) + count * 86400000).toISOString().slice(0, 10);
export const daysUntil = (day, today) => Math.round((Date.parse(day) - Date.parse(today)) / 86400000);
const yearAhead = day => {
  const next = (Number(day.slice(0, 4)) + 1) + day.slice(4);
  return validDay(next) ? next : next.slice(0, 4) + '-02-28';
};
function title(value) {
  if (typeof value !== 'string' || !value.trim() || value.length > 160 ||
      /[\u0000-\u001f\u007f@]|\b\d{4}\b|\b\d{1,2}[/.\-]\d{1,2}\b/u.test(value)) throw Error('Invalid calendar title');
  return value.trim();
}
function occurrences(rows) {
  if (!Array.isArray(rows) || rows.length > CALENDAR_LIMITS.items) throw Error('Invalid calendar rows');
  const ids = new Set();
  return rows.map(row => {
    if (!row || !/^[a-f0-9]{64}$/.test(row.occurrenceId) || ids.has(row.occurrenceId) ||
        !validDay(row.localDate) || !['birthday', 'occasion'].includes(row.kind)) throw Error('Invalid calendar row');
    ids.add(row.occurrenceId);
    const item = { occurrenceId: row.occurrenceId, title: title(row.title), kind: row.kind, localDate: row.localDate };
    if (row.displayAge !== undefined) {
      if (!Number.isInteger(row.displayAge) || row.displayAge < 0 || row.displayAge > 130 || row.kind !== 'birthday') throw Error('Invalid display age');
      item.displayAge = row.displayAge;
    }
    return item;
  }).sort((a, b) => a.localDate.localeCompare(b.localDate) || a.occurrenceId.localeCompare(b.occurrenceId));
}
// Only the sanitised shape is persisted; raw descriptions, IDs and owners never reach TV.
export function restoreCalendar(value, source) {
  try {
    if (value?.source !== source || value.schemaVersion !== 1 || value.timezone !== ZONE ||
        typeof value.fetchedAt !== 'string' || !/Z$/.test(value.fetchedAt) || !Number.isFinite(Date.parse(value.fetchedAt))) return null;
    const next = { source, schemaVersion: 1, timezone: ZONE, fetchedAt: value.fetchedAt, occurrences: occurrences(value.occurrences) };
    if (Buffer.byteLength(JSON.stringify(next)) > CALENDAR_LIMITS.snapshotBytes) return null;
    return next;
  } catch { return null; }
}
export function calendarView(snapshot, now = new Date()) {
  const today = dayAt(now), age = now - Date.parse(snapshot?.fetchedAt);
  return { schemaVersion: 1, timezone: ZONE, fetchedAt: snapshot?.fetchedAt ?? null,
    stale: !Number.isFinite(age) || age < 0 || age >= 86400000,
    occurrences: (snapshot?.occurrences ?? []).filter(row => row.localDate >= today && row.localDate <= yearAhead(today))
      .map(row => ({ ...row, daysUntil: daysUntil(row.localDate, today) })) };
}
async function jsonGet(url, token, fetcher) {
  const response = await fetcher(url, { method: 'GET', headers: { Authorization: 'Bearer ' + token, Prefer: 'outlook.timezone="AUS Eastern Standard Time", outlook.body-content-type="text"' },
    redirect: 'error', signal: AbortSignal.timeout(15000) });
  if (!response.ok) throw Error('Calendar request failed');
  let size = 0; const chunks = [];
  for await (const chunk of response.body) {
    size += chunk.length;
    if (size > CALENDAR_LIMITS.bytes) throw Error('Calendar response too large');
    chunks.push(Buffer.from(chunk));
  }
  return JSON.parse(Buffer.concat(chunks).toString('utf8'));
}
export function calendarSource(config) {
  return createHash('sha256').update(JSON.stringify([config.outlookClientId, config.calendarId, config.calendarAges === true])).digest('hex');
}
export async function fetchCalendar(config, fetcher = fetch, now = new Date()) {
  const { calendarId, getAccessToken } = config;
  if (typeof calendarId !== 'string' || !calendarId || calendarId.length > 2048 || typeof getAccessToken !== 'function') throw Error('Configure Outlook calendar');
  const today = dayAt(now), token = await getAccessToken();
  const route = '/v1.0/me/calendars/' + encodeURIComponent(calendarId) + '/calendarView';
  const query = new URLSearchParams({ startDateTime: addDays(today, -1) + 'T00:00:00Z',
    endDateTime: addDays(today, 401) + 'T00:00:00Z', '$top': '100',
    '$select': 'id,subject,isAllDay,start,end,isCancelled' + (config.calendarAges === true ? ',body' : '') });
  let url = 'https://graph.microsoft.com' + route + '?' + query;
  const visited = new Set(), events = []; let received = 0;
  while (url) {
    const parsed = new URL(url);
    if (parsed.origin !== 'https://graph.microsoft.com' || parsed.pathname !== route || parsed.hash ||
        parsed.username || parsed.password || visited.has(url) || visited.size >= 10) throw Error('Invalid calendar pagination');
    visited.add(url);
    const page = await jsonGet(url, token, fetcher);
    if (!Array.isArray(page?.value) || (received += page.value.length) > CALENDAR_LIMITS.items) throw Error('Invalid calendar events');
    for (const row of page.value) {
      if (row?.isCancelled === true) continue;
      const civil = value => {
        if (value?.timeZone !== 'AUS Eastern Standard Time' && value?.timeZone !== ZONE) throw Error('Unexpected all-day timezone');
        if (typeof value.dateTime !== 'string' || !/^\d{4}-\d{2}-\d{2}T00:00:00(?:\.0+)?$/.test(value.dateTime)) throw Error('Invalid all-day time');
        return value.dateTime.slice(0, 10);
      };
      if (row?.isAllDay !== true || typeof row.isCancelled !== 'boolean' || typeof row.id !== 'string' || !row.id || row.id.length > 1024) throw Error('Invalid calendar event');
      events.push({ uid: row.id, summary: row.subject, start: { date: civil(row.start) }, end: { date: civil(row.end) },
        description: row.body?.contentType === 'text' ? row.body.content : undefined });
    }
    url = page['@odata.nextLink'] ?? null;
    if (url !== null && (typeof url !== 'string' || !url)) throw Error('Invalid calendar pagination');
  }
  const unique = new Map();
  for (const event of events) {
    // Dedicated occasions calendar: reject timed/multi-day records rather than guess dates.
    const day = event?.start?.date, end = event?.end?.date;
    if (!validDay(day) || !validDay(end) || end !== addDays(day, 1)) throw Error('Expected one-day all-day occasion');
    if (day < today || day > addDays(today, 400)) continue;
    const label = title(event.summary);
    const kind = /birthday/i.test(label) ? 'birthday' : 'occasion';
    const occurrenceId = createHash('sha256').update(JSON.stringify([calendarSource(config), event.uid, day])).digest('hex');
    const item = { occurrenceId, title: label, kind, localDate: day };
    // Optional exact private metadata line, never an inferred age or raw description.
    if (config.calendarAges === true && kind === 'birthday' && typeof event.description === 'string') {
      const match = /^ambient:birth-year=(\d{4})$/m.exec(event.description);
      const age = match ? Number(day.slice(0, 4)) - Number(match[1]) : -1;
      if (age >= 0 && age <= 130) item.displayAge = age;
    }
    if (unique.has(occurrenceId) && JSON.stringify(unique.get(occurrenceId)) !== JSON.stringify(item)) throw Error('Conflicting calendar duplicate');
    unique.set(occurrenceId, item);
  }
  const next = restoreCalendar({ source: calendarSource(config), schemaVersion: 1, timezone: ZONE,
    fetchedAt: now.toISOString(), occurrences: [...unique.values()] }, calendarSource(config));
  if (!next) throw Error('Invalid calendar snapshot');
  return next;
}
export class CalendarFeed {
  constructor(config, saved, persist, fetcher = fetch) {
    this.config = config; this.snapshot = config.calendarId ? restoreCalendar(saved, calendarSource(config)) : null;
    this.persist = persist; this.fetcher = fetcher; this.busy = false;
  }
  async update(now = new Date()) {
    if (this.busy || !this.config.getAccessToken || !this.config.calendarId) return;
    this.busy = true;
    try {
      const next = await fetchCalendar(this.config, this.fetcher, now);
      await this.persist(next); this.snapshot = next;
    } finally { this.busy = false; }
  }
  view(now = new Date()) { return calendarView(this.snapshot, now); }
}
