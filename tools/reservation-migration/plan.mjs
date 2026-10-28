// Pure planner. No SDK, credentials, filesystem access or network calls.
import { createCipheriv, createDecipheriv, createHash, createHmac, randomBytes } from 'node:crypto';
import { DEFAULT_TARGET, resolveTarget, assertSameTarget } from './targets.mjs';

export const PROJECT = DEFAULT_TARGET.project; // Existing-target exports retained for compatibility.
export const DATABASE = DEFAULT_TARGET.database;
export const COLLECTIONS = ['bookings', 'slots', 'garages', 'services', 'vehicles', 'users'];
export const POLICY = 'reservationPolicies/current';
export const OFFSET = 19_800_000;
export const MANIFEST_FORMAT = 2; // Requires guards for written AND unchanged pair members.
const SLOT_KEYS = new Set(['id', 'garageId', 'scheduledDate', 'dateString', 'scheduledTime', 'isReserved', 'reserved', 'bookingId', 'status', 'updatedAt', 'timeZoneId', 'reservationVersion']);
const TIME = /^(0[1-9]|1[0-2]):[0-5][0-9] (AM|PM)$/;
export const stable = value => JSON.stringify(value, (_, item) => item && typeof item === 'object' && !Array.isArray(item)
  ? Object.fromEntries(Object.keys(item).sort().map(k => [k, item[k]])) : item);
export const equal = (a, b) => stable(a) === stable(b);
export function keyFromHex(hex) {
  if (!/^[a-fA-F0-9]{64}$/.test(hex ?? '')) throw new Error('KEY_REQUIRED_32_BYTES_HEX');
  return Buffer.from(hex, 'hex');
}
export const protectedRef = (key, path) => createHmac('sha256', key).update(`ref:${path}`).digest('hex');
const digest = (key, value) => createHmac('sha256', key).update(`manifest:${stable(value)}`).digest('hex');
export function seal(value, key) {
  const iv = randomBytes(12);
  const cipher = createCipheriv('aes-256-gcm', createHmac('sha256', key).update('encryption').digest(), iv);
  cipher.setAAD(Buffer.from('autocare-reservation-rehearsal-v1'));
  const ciphertext = Buffer.concat([cipher.update(stable(value)), cipher.final()]);
  return { format: 1, iv: iv.toString('hex'), tag: cipher.getAuthTag().toString('hex'), ciphertext: ciphertext.toString('base64') };
}
export function unseal(envelope, key) {
  if (envelope.format !== 1) throw new Error('INVALID_ENVELOPE');
  const decipher = createDecipheriv('aes-256-gcm', createHmac('sha256', key).update('encryption').digest(), Buffer.from(envelope.iv, 'hex'));
  decipher.setAAD(Buffer.from('autocare-reservation-rehearsal-v1'));
  decipher.setAuthTag(Buffer.from(envelope.tag, 'hex'));
  return JSON.parse(Buffer.concat([decipher.update(Buffer.from(envelope.ciphertext, 'base64')), decipher.final()]).toString());
}
export function encode(value) {
  if (value === null) return { nullValue: null };
  if (typeof value === 'boolean') return { booleanValue: value };
  if (typeof value === 'number') return Number.isSafeInteger(value) ? { integerValue: String(value) } : { doubleValue: value };
  if (typeof value === 'string') return { stringValue: value };
  if (Array.isArray(value)) return { arrayValue: { values: value.map(encode) } };
  return { mapValue: { fields: fields(value) } };
}
export const fields = object => Object.fromEntries(Object.entries(object).map(([k, v]) => [k, encode(v)]));
function value(v) {
  if ('integerValue' in v) { const n = Number(v.integerValue); return Number.isSafeInteger(n) ? n : v.integerValue; }
  if ('mapValue' in v) return decode(v.mapValue.fields);
  if ('arrayValue' in v) return (v.arrayValue.values ?? []).map(value);
  return v.stringValue ?? v.booleanValue ?? v.doubleValue ?? v.timestampValue ?? v.referenceValue ?? null;
}
export const decode = object => Object.fromEntries(Object.entries(object ?? {}).map(([k, v]) => [k, value(v)]));
const patch = (original, changes) => ({ ...original, ...fields(changes) });
export function identity(garageId, dateString, scheduledTime) {
  if (!/^[A-Za-z0-9_-]{1,128}$/.test(garageId ?? '') || !/^\d{4}-\d{2}-\d{2}$/.test(dateString ?? '') || !TIME.test(scheduledTime ?? '')) throw new Error('INVALID_IDENTITY');
  const utc = Date.parse(`${dateString}T00:00:00.000Z`);
  if (!Number.isFinite(utc) || new Date(utc).toISOString().slice(0, 10) !== dateString || utc - OFFSET <= 0) throw new Error('INVALID_DATE');
  const canonical = `${garageId.length}:${garageId}:10:${dateString}:${scheduledTime.length}:${scheduledTime}`;
  const hash = createHash('sha256').update(canonical).digest('hex').slice(0, 16);
  const id = `slot_${garageId.replace(/[^a-zA-Z0-9]/g, '_').slice(0, 24)}_${dateString}_${scheduledTime.replace(/[^a-zA-Z0-9]/g, '')}_${hash}`;
  return { id, garageId, dateString, scheduledTime, scheduledDate: utc - OFFSET, timeZoneId: 'Asia/Kolkata', reservationVersion: 2 };
}
function canonical(data) {
  try { const i = identity(data.garageId, data.dateString, data.scheduledTime); return data.reservationVersion === 2 && data.timeZoneId === i.timeZoneId && data.scheduledDate === i.scheduledDate ? i : null; }
  catch { return null; }
}
const idPart = id => typeof id === 'string' && /^[^/\x00-\x1f]{1,128}$/.test(id);
export function plan(snapshot, decisions, key) {
  const target = resolveTarget(snapshot);
  const baseline = snapshot.docs;
  if (Object.keys(baseline).length > 200 || Buffer.byteLength(stable(snapshot)) > 200_000) throw new Error('REHEARSAL_INVENTORY_LIMIT');
  const issues = [], entries = [], claimed = new Map(), handled = new Set();
  const issue = (code, path, blocking = true) => issues.push({ code, ref: protectedRef(key, path), blocking });
  const get = path => baseline[path] ? decode(baseline[path].fields) : null;
  const decision = path => {
    const d = decisions[path];
    if (!d || typeof d.approvedBy !== 'string' || !d.approvedBy.trim() || typeof d.evidence !== 'string' || !d.evidence.trim()) { issue('APPOINTMENT_EVIDENCE_REQUIRED', path); return null; }
    return d;
  };
  const resolve = (path, data) => {
    const valid = canonical(data);
    if (valid) return valid;
    if (data.reservationVersion === 2) { issue('INCONSISTENT_V2_IDENTITY', path); return null; }
    const d = decision(path);
    if (!d) return null;
    try { return identity(data.garageId, d.dateString, d.scheduledTime); }
    catch { issue('INVALID_APPROVED_IDENTITY', path); return null; }
  };
  const operation = (path, after) => ({ path, ref: protectedRef(key, path), before: baseline[path] ?? null, after });
  const add = (source, target, operations, reasons, dependencies = []) => {
    if (claimed.has(target)) { issue('CONFLICTING_CANONICAL_TARGET', source); issue('CONFLICTING_CANONICAL_TARGET', claimed.get(target)); }
    claimed.set(target, source);
    const changes = operations.filter(op => !op.before || !equal(op.before.fields, op.after));
    const changedPaths = new Set(changes.map(op => op.path));
    // Keep the complete booking/canonical-slot/retired-slot read set even when
    // no write is needed. Do not force no-op writes just to obtain a new version.
    const guards = operations.map(op => ({ path: op.path, ref: op.ref, expectedVersion: changedPaths.has(op.path)
      ? { source: 'applyCommit' }
      : { source: 'baseline', updateTime: op.before.updateTime } })).sort((a, b) => a.path.localeCompare(b.path));
    if (changes.length) entries.push({ id: protectedRef(key, source), ref: protectedRef(key, source), reasons, dependencies: [...new Set(dependencies)].sort(), guards, operations: changes });
    else issue('CANONICAL_UNCHANGED', source, false);
  };
  const validSlot = path => {
    const s = get(path);
    if (Object.keys(s).some(k => !SLOT_KEYS.has(k))) { issue('UNEXPECTED_PUBLIC_SLOT_FIELDS', path); return null; }
    if (s.id !== path.slice(6)) { issue('SLOT_DOCUMENT_ID_MISMATCH', path); return null; }
    if ('reserved' in s && 'isReserved' in s && s.reserved !== s.isReserved) { issue('CONFLICTING_RESERVED_ALIASES', path); return null; }
    const reserved = s.isReserved ?? s.reserved;
    if (typeof reserved !== 'boolean' || (reserved ? s.status !== 'reserved' || !idPart(s.bookingId) : s.status !== 'available' || s.bookingId !== '')) { issue('INVALID_SLOT_STATE', path); return null; }
    return { ...s, isReserved: reserved };
  };
  const slotFields = (i, bookingId, reserved, updatedAt) => fields({ ...i, bookingId, isReserved: reserved, status: reserved ? 'reserved' : 'available', updatedAt });
  const retirement = (source, oldPath, d) => {
    if (d?.retireLegacySlot !== true) { issue('EXPLICIT_RETIREMENT_APPROVAL_REQUIRED', source); return null; }
    // Keep the old record and all its original fields. "retired" fails closed in app/rules.
    return operation(oldPath, patch(baseline[oldPath].fields, { isReserved: false, status: 'retired', bookingId: '' }));
  };
  const paths = Object.keys(baseline).sort();
  for (const path of paths.filter(p => p.startsWith('bookings/'))) {
    const b = get(path);
    if (b.id !== path.slice(9)) { issue('BOOKING_DOCUMENT_ID_MISMATCH', path); continue; }
    if (['cancelled', 'completed'].includes(b.status)) { issue('TERMINAL_HISTORY_UNCHANGED', path, false); continue; }
    if (!['pending', 'confirmed', 'in_progress'].includes(b.status)) { issue('UNKNOWN_BOOKING_STATUS', path); continue; }
    const i = resolve(path, b);
    if (!i) continue;
    let badDependency = false;
    for (const [collection, id] of [['garages', b.garageId], ['users', b.customerId], ['services', b.serviceId], ['vehicles', b.vehicleId]]) {
      if (!idPart(id) || !get(`${collection}/${id}`)) { issue('MISSING_OR_INVALID_DEPENDENCY', path); badDependency = true; }
    }
    if (badDependency) continue;
    const g = get(`garages/${b.garageId}`), u = get(`users/${b.customerId}`), s = get(`services/${b.serviceId}`), v = get(`vehicles/${b.vehicleId}`);
    const owner = idPart(g.ownerId) ? get(`users/${g.ownerId}`) : null;
    if (!['CUSTOMER', 'customer'].includes(u.role) || !owner || !['GARAGE', 'garage', 'garage_owner'].includes(owner.role) || s.garageId !== b.garageId || v.ownerId !== b.customerId) { issue('OWNERSHIP_OR_ROLE_REVIEW_REQUIRED', path); continue; }
    const linked = paths.filter(p => p.startsWith('slots/') && get(p).bookingId === b.id && get(p).status !== 'retired');
    if (linked.length > 1) { issue('MULTIPLE_SLOTS_FOR_BOOKING', path); continue; }
    let oldPath = b.slotId ? `slots/${b.slotId}` : linked[0];
    if (oldPath && !baseline[oldPath]) oldPath = undefined;
    if (linked.length && oldPath !== linked[0]) { issue('BOOKING_SLOT_LINK_MISMATCH', path); continue; }
    let oldSlot;
    if (oldPath) {
      handled.add(oldPath);
      oldSlot = validSlot(oldPath);
      if (!oldSlot) continue;
      if (!oldSlot.isReserved || oldSlot.bookingId !== b.id || oldSlot.garageId !== b.garageId || oldSlot.scheduledTime !== b.scheduledTime || oldSlot.scheduledDate !== b.scheduledDate) { issue('BOOKING_SLOT_IDENTITY_MISMATCH', path); continue; }
      if (canonical(b) && (!canonical(oldSlot) || oldSlot.id !== i.id || !equal(canonical(oldSlot), i))) { issue('INCONSISTENT_V2_PAIR', path); continue; }
    } else {
      const d = decision(path);
      if (d?.createMissingSlot !== true) { issue('MISSING_SLOT_APPROVAL_REQUIRED', path); continue; }
    }
    const target = `slots/${i.id}`;
    if (baseline[target] && target !== oldPath) { issue('CANONICAL_TARGET_ALREADY_EXISTS', path); continue; }
    const operations = [operation(path, patch(baseline[path].fields, { slotId: i.id, dateString: i.dateString, scheduledDate: i.scheduledDate, scheduledTime: i.scheduledTime, timeZoneId: i.timeZoneId, reservationVersion: 2 })), operation(target, slotFields(i, b.id, true, Number.isSafeInteger(oldSlot?.updatedAt) ? oldSlot.updatedAt : 0))];
    if (oldPath && oldPath !== target) {
      const retired = retirement(path, oldPath, decision(path));
      if (!retired) continue;
      operations.push(retired);
    }
    add(path, target, operations, ['VERIFIED_ACTIVE_APPOINTMENT', oldPath ? 'NORMALIZE_RESERVATION' : 'APPROVED_MISSING_RESERVATION'], [`garages/${b.garageId}`, `users/${b.customerId}`, `users/${g.ownerId}`, `services/${b.serviceId}`, `vehicles/${b.vehicleId}`]);
  }
  for (const path of paths.filter(p => p.startsWith('slots/') && !handled.has(p))) {
    const raw = get(path);
    if (raw.status === 'retired') {
      if (Object.keys(raw).some(k => !SLOT_KEYS.has(k)) || raw.isReserved !== false || raw.bookingId !== '') issue('INVALID_RETIRED_RECORD', path);
      else issue('RETIRED_RECORD_REQUIRES_SIGNOFF', path, false);
      continue;
    }
    const s = validSlot(path);
    if (!s) continue;
    if (s.isReserved) {
      const b = get(`bookings/${s.bookingId}`);
      if (!b) issue('ORPHAN_RESERVED_SLOT', path);
      else if (b.status === 'cancelled') issue('CANCELLED_BOOKING_STILL_RESERVED', path);
      else if (b.status === 'completed') issue('COMPLETED_RESERVATION_RETAINED', path, false);
      else issue('UNRESOLVED_ACTIVE_SLOT', path);
      continue;
    }
    const i = resolve(path, s);
    if (!i) continue;
    if (!get(`garages/${s.garageId}`)) { issue('MISSING_GARAGE', path); continue; }
    const target = `slots/${i.id}`;
    if (target !== path && baseline[target]) { issue('CANONICAL_TARGET_ALREADY_EXISTS', path); continue; }
    const operations = [operation(target, slotFields(i, '', false, Number.isSafeInteger(s.updatedAt) ? s.updatedAt : 0))];
    if (target !== path) {
      const retired = retirement(path, path, decision(path));
      if (!retired) continue;
      operations.push(retired);
    }
    add(path, target, operations, ['VERIFIED_AVAILABLE_SLOT'], [`garages/${s.garageId}`]);
  }
  if (decode(snapshot.policy?.fields).legacyMigrationComplete === true) issue('ROLLOUT_MUST_BE_DISABLED', POLICY);
  const writes = entries.flatMap(e => e.operations.map(op => op.path));
  if (new Set(writes).size !== writes.length) issue('OVERLAPPING_MIGRATION_UNITS', POLICY);
  const core = { format: MANIFEST_FORMAT, project: target.project, database: target.database, emulatorHost: target.emulatorHost, baseline, policy: snapshot.policy, decisions, entries: entries.sort((a, b) => a.id.localeCompare(b.id)), issues: issues.sort((a, b) => stable(a).localeCompare(stable(b))) };
  return { ...core, hash: digest(key, core) };
}
export function validateManifest(manifest, key, expectedTarget) {
  const target = resolveTarget(manifest);
  if (expectedTarget !== undefined) assertSameTarget(target, expectedTarget);
  const { hash, ...core } = manifest;
  if (hash !== digest(key, core) || manifest.format !== MANIFEST_FORMAT || manifest.emulatorHost !== target.emulatorHost) throw new Error('MANIFEST_INTEGRITY_OR_ENVIRONMENT_FAILURE');
  // Recompute proposals rather than accepting arbitrary write paths/payloads.
  const rebuilt = plan({ project: target.project, database: target.database, emulatorHost: target.emulatorHost, docs: manifest.baseline, policy: manifest.policy }, manifest.decisions, key);
  if (!equal(manifest, rebuilt)) throw new Error('MANIFEST_RECOMPUTATION_FAILED');
}
export const summary = manifest => ({ manifestHash: manifest.hash, units: manifest.entries.length, writes: manifest.entries.reduce((n, e) => n + e.operations.length, 0), blockers: manifest.issues.filter(i => i.blocking).length, issues: manifest.issues });
