// Synthetic fixtures only. Never an export/import path for customer data.
import { fields, identity, POLICY } from '../../tools/reservation-migration/plan.mjs';
import { Emulator } from '../../tools/reservation-migration/emulator.mjs';
import { DEFAULT_TARGET, resolveTarget } from '../../tools/reservation-migration/targets.mjs';

export const TEST_KEY_HEX = '31'.repeat(32); // PUBLIC fixture key, not a credential or real audit key.
export function fixture({ count = 1, canonical = false, oldId = false, missing = false } = {}) {
  const docs = {
    'users/customer_fixture': { role: 'CUSTOMER' }, 'users/owner_fixture': { role: 'GARAGE' },
    'garages/garage_apex': { id: 'garage_apex', ownerId: 'owner_fixture' },
    'services/service_fixture': { id: 'service_fixture', garageId: 'garage_apex', price: 799, available: true, durationMinutes: 60 },
    'vehicles/vehicle_fixture': { id: 'vehicle_fixture', ownerId: 'customer_fixture' }
  };
  const decisions = {}, bookings = [], slots = [];
  for (let n = 1; n <= count; n++) {
    const dateString = `2031-08-${String(n + 5).padStart(2, '0')}`;
    const i = identity('garage_apex', dateString, '11:00 AM');
    const b = `booking_fixture_${n}`, s = oldId ? `legacy_slot_${n}` : i.id;
    bookings.push(`bookings/${b}`); slots.push(`slots/${s}`);
    docs[`bookings/${b}`] = { id: b, customerId: 'customer_fixture', garageId: i.garageId, serviceId: 'service_fixture', vehicleId: 'vehicle_fixture', status: 'pending', scheduledDate: canonical ? i.scheduledDate : Date.parse(`${dateString}T00:00:00Z`), scheduledTime: i.scheduledTime, totalAmount: 799, updatedAt: 100, createdAt: 100, customerName: 'DO_NOT_LOG_SYNTHETIC_PERSON', notes: 'synthetic-only', ...(canonical ? { slotId: i.id, dateString: i.dateString, timeZoneId: i.timeZoneId, reservationVersion: 2 } : {}) };
    if (!missing) docs[`slots/${s}`] = { ...(canonical ? i : { id: s, garageId: i.garageId, scheduledDate: docs[`bookings/${b}`].scheduledDate, scheduledTime: i.scheduledTime, dateString }), bookingId: b, ...(canonical ? { isReserved: true } : { reserved: true }), status: 'reserved', updatedAt: 100 };
    if (!canonical || missing) decisions[`bookings/${b}`] = { dateString, scheduledTime: i.scheduledTime, approvedBy: 'synthetic-reviewer', evidence: `synthetic-appointment-${n}`, ...(oldId ? { retireLegacySlot: true } : {}), ...(missing ? { createMissingSlot: true } : {}) };
  }
  return { docs, decisions, bookings, slots, policy: { version: 2, timeZoneId: 'Asia/Kolkata', legacyMigrationComplete: false } };
}
export function slotOnlyFixture(options = {}) {
  const f = fixture({ ...options, canonical: true });
  // A canonical booking whose slot still has the legacy serialized alias.
  // Identity is already correct; only slot serialization needs a migration write.
  for (const path of f.slots) f.docs[path].reserved = true;
  return f;
}
export function bookingOnlyFixture() {
  const f = fixture({ canonical: true }), b = f.docs[f.bookings[0]], s = f.docs[f.slots[0]];
  for (const field of ['slotId', 'dateString', 'timeZoneId', 'reservationVersion']) delete b[field];
  f.decisions[f.bookings[0]] = { dateString: s.dateString, scheduledTime: s.scheduledTime, approvedBy: 'synthetic-reviewer', evidence: 'synthetic-normalized-date' };
  return f;
}
export function offlineSnapshot(f = fixture(), target = DEFAULT_TARGET) {
  const selected = resolveTarget(target);
  return { project: selected.project, database: selected.database, emulatorHost: selected.emulatorHost, docs: Object.fromEntries(Object.entries(f.docs).map(([p, d]) => [p, { fields: fields(d), updateTime: '2030-01-01T00:00:00.000000001Z' }])), policy: { fields: fields(f.policy), updateTime: '2030-01-01T00:00:00.000000001Z' } };
}
export const emulator = (target = DEFAULT_TARGET) => new Emulator(target);
export function fixtureWrites(f, target = DEFAULT_TARGET) {
  const root = resolveTarget(target).root;
  return Object.entries({ ...f.docs, [POLICY]: f.policy }).map(([path, data]) => ({ update: { name: `${root}/${path}`, fields: fields(data) }, currentDocument: { exists: false } }));
}
export async function seed(db, f) {
  const target = resolveTarget(db.target);
  return db.request(`/v1/${target.root}:commit`, { writes: fixtureWrites(f, target) });
}
