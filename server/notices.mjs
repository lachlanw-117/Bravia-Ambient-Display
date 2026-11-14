// This adapter reads the TV-filtered view only, never the private attention store.
export const NOTICE_LIMITS = { bytes: 262144, items: 64, title: 300 };
const text = (value, max) => {
  if (typeof value !== 'string' || !value.trim() || value.length > max || /[\u0000-\u001f\u007f]/u.test(value))
    throw Error('Invalid notice text');
  return value.trim();
};
const stamp = (value, nullable = false) => {
  if (nullable && value === null) return null;
  if (typeof value !== 'number' || !Number.isFinite(value) || value <= 0 || value > 8640000000000)
    throw Error('Invalid notice timestamp');
  return value;
};
export function sanitiseItems(items) {
  if (!Array.isArray(items) || items.length > NOTICE_LIMITS.items) throw Error('Invalid notices');
  const seen = new Set();
  return items.map(row => {
    if (!row || typeof row !== 'object' || typeof row.active !== 'boolean') throw Error('Invalid notice');
    const occurrenceId = text(row.occurrence_id, 256);
    if (seen.has(occurrenceId)) throw Error('Duplicate notice');
    seen.add(occurrenceId);
    if (!['important','warning','info'].includes(row.severity) || !['fault','event','attention'].includes(row.kind) ||
        !['valid','unknown','stale'].includes(row.quality)) throw Error('Unsupported notice');
    let title = text(row.title, NOTICE_LIMITS.title);
    const item = { occurrenceId, category: text(row.category, 64), kind: row.kind, severity: row.severity,
      title, active: row.active, firstDetected: stamp(row.first_detected),
      lastValidObservation: stamp(row.last_valid_observation, true), resolvedAt: stamp(row.resolved_at, true),
      expiresAt: row.kind === 'event' ? stamp(row.display_until) : null, quality: row.quality };
    if (row.key === 'upgrades_available') {
      if (!Array.isArray(row.details) || row.details.length > 256) throw Error('Invalid upgrade details');
      const components = new Set(row.details.map(detail => text(detail?.entity_id, 256)));
      item.updateCount = components.size;
      item.title = item.updateCount ? `${item.updateCount} update${item.updateCount === 1 ? '' : 's'} available` : 'Update status uncertain';
      if (item.updateCount && item.quality !== 'valid') item.title += ' · confirmation incomplete';
    }
    return item;
  });
}
export function noticesView(snapshot, now = new Date()) {
  const age = now.getTime() - Date.parse(snapshot?.fetchedAt);
  return { version: 1, fetchedAt: snapshot?.fetchedAt ?? null,
    stale: !Number.isFinite(age) || age < 0 || age >= 120000,
    items: (snapshot?.items ?? []).filter(item => item.kind === 'event'
      ? item.expiresAt * 1000 > now.getTime() : item.active) };
}
export async function fetchNotices({ haUrl, haToken }, fetcher = fetch, now = new Date()) {
  const response = await fetcher(haUrl.replace(/\/$/, '') + '/api/states/sensor.household_attention_tv', {
    method: 'GET', headers: { Authorization: 'Bearer ' + haToken },
    redirect: 'error', signal: AbortSignal.timeout(10000)
  });
  if (!response.ok) throw Error('Notice request failed');
  // Bound the stream before parsing, including chunked responses without Content-Length.
  let size = 0; const chunks = [];
  for await (const chunk of response.body) {
    size += chunk.length;
    if (size > NOTICE_LIMITS.bytes) throw Error('Notice response too large');
    chunks.push(Buffer.from(chunk));
  }
  const state = JSON.parse(Buffer.concat(chunks).toString('utf8'));
  if (state.state !== 'ready' || state.attributes?.schema_version !== 1) throw Error('Notice feed not ready');
  const snapshot = { version: 1, fetchedAt: now.toISOString(), items: sanitiseItems(state.attributes.items) };
  if (Buffer.byteLength(JSON.stringify(snapshot)) > 60000) throw Error('Sanitised notices too large');
  return snapshot;
}
// Private persisted snapshots have the public shape, not HA's raw records.
export function restoreNotices(value) {
  if (value?.version !== 1 || !Number.isFinite(Date.parse(value.fetchedAt))) return null;
  try {
    const items = sanitiseItems(value.items.map(item => ({
      occurrence_id: item.occurrenceId, category: item.category, kind: item.kind,
      severity: item.severity, title: item.title, active: item.active,
      first_detected: item.firstDetected, last_valid_observation: item.lastValidObservation,
      resolved_at: item.resolvedAt, display_until: item.expiresAt, quality: item.quality
    })));
    items.forEach((item, index) => {
      const count = value.items[index].updateCount;
      if (count !== undefined) {
        if (!Number.isInteger(count) || count < 0 || count > 256) throw Error('Invalid upgrade count');
        item.updateCount = count;
      }
    });
    const snapshot = { version: 1, fetchedAt: value.fetchedAt, items };
    if (Buffer.byteLength(JSON.stringify(snapshot)) > 60000) return null;
    return snapshot;
  } catch { return null; }
}

export class NoticeFeed {
  constructor(config, saved, persist, fetcher = fetch) {
    this.config = config; this.snapshot = restoreNotices(saved); this.persist = persist;
    this.fetcher = fetcher; this.busy = false;
  }
  async update(now = new Date()) {
    if (this.busy || !this.config.haToken) return;
    this.busy = true;
    try {
      const next = await fetchNotices(this.config, this.fetcher, now);
      await this.persist(next); this.snapshot = next;
    } finally { this.busy = false; }
  }
  view(now = new Date()) { return noticesView(this.snapshot, now); }
}
