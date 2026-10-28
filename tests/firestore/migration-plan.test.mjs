import { test } from 'node:test';
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { Emulator } from '../../tools/reservation-migration/emulator.mjs';
import { decode, fields, identity, keyFromHex, plan, seal, stable, summary, unseal, validateManifest } from '../../tools/reservation-migration/plan.mjs';
import { bookingOnlyFixture, fixture, offlineSnapshot, slotOnlyFixture, TEST_KEY_HEX } from './migration-fixtures.mjs';
const key = keyFromHex(TEST_KEY_HEX);
const codes = m => m.issues.filter(i => i.blocking).map(i => i.code);

test('slot ID matches the Kotlin legacy-format regression vector', () => {
  assert.equal(identity('garage_apex', '2026-10-30', '11:00 AM').id, 'slot_garage_apex_2026-10-30_1100AM_9c2f95b7ef65bbc5');
  assert.equal(new Date(identity('garage_apex', '2026-10-30', '11:00 AM').scheduledDate).toISOString(), '2026-10-29T18:30:00.000Z');
});
test('invalid dates/time/garage IDs never normalize by guessing', () => {
  for (const [g, d, t] of [['g', '2031-02-30', '11:00 AM'], ['g', '2031-01-01', '11:00 am'], ['a/b', '2031-01-01', '11:00 AM']]) assert.throws(() => identity(g, d, t));
});
test('timezone does not depend on device/process TZ', () => {
  const url = new URL('../../tools/reservation-migration/plan.mjs', import.meta.url).href;
  const source = `import {identity} from ${JSON.stringify(url)};console.log(JSON.stringify(identity('garage_apex','2031-08-06','11:00 AM')))`;
  const result = ['UTC', 'Asia/Kolkata', 'Pacific/Honolulu'].map(TZ => {
    const child = spawnSync(process.execPath, ['--input-type=module', '-e', source], { env: { ...process.env, TZ }, encoding: 'utf8' });
    assert.equal(child.status, 0); return child.stdout;
  });
  assert.equal(new Set(result).size, 1);
});
test('unknown project, database and host are rejected before requests', () => {
  for (const options of [{}, { project: 'real-project', database: '(default)' }, { project: 'demo-autocare-migration', database: 'production' }, { project: 'demo-autocare-migration', database: '(default)', emulatorHost: 'firestore.googleapis.com' }]) assert.throws(() => new Emulator(options), /ENVIRONMENT_REFUSED/);
});
test('manifest is deterministic across input document order; slot and booking IDs preserved', () => {
  const s = offlineSnapshot(), original = stable(s);
  const m = plan(s, fixture().decisions, key);
  const shuffled = { ...s, docs: Object.fromEntries(Object.entries(s.docs).reverse()) };
  assert.equal(stable(m), stable(plan(shuffled, fixture().decisions, key)));
  assert.deepEqual(codes(m), []); assert.equal(m.entries.length, 1);
  assert.equal(stable(s), original);
  assert.ok(m.entries[0].operations.every(op => op.before !== null));
  validateManifest(m, key);
});
test('slot-only write retains the unchanged booking baseline version as a pair guard', () => {
  const f = slotOnlyFixture(), snapshot = offlineSnapshot(f), m = plan(snapshot, f.decisions, key);
  assert.deepEqual(codes(m), []); assert.equal(m.format, 2);
  const entry = m.entries[0];
  assert.deepEqual(entry.operations.map(op => op.path), f.slots);
  assert.deepEqual(entry.guards.find(g => g.path === f.bookings[0]).expectedVersion, { source: 'baseline', updateTime: snapshot.docs[f.bookings[0]].updateTime });
  assert.deepEqual(entry.guards.find(g => g.path === f.slots[0]).expectedVersion, { source: 'applyCommit' });
  validateManifest(m, key);
});
test('booking-only write retains the unchanged canonical slot version as a pair guard', () => {
  const f = bookingOnlyFixture(), snapshot = offlineSnapshot(f), m = plan(snapshot, f.decisions, key);
  assert.deepEqual(codes(m), []);
  assert.deepEqual(m.entries[0].operations.map(op => op.path), f.bookings);
  assert.deepEqual(m.entries[0].guards.find(g => g.path === f.slots[0]).expectedVersion, { source: 'baseline', updateTime: snapshot.docs[f.slots[0]].updateTime });
});
test('renames guard the booking, canonical slot and retained legacy slot', () => {
  const f = fixture({ oldId: true }), m = plan(offlineSnapshot(f), f.decisions, key), entry = m.entries[0];
  assert.equal(entry.guards.length, 3);
  assert.deepEqual(entry.guards.map(g => g.path).sort(), entry.operations.map(op => op.path).sort());
  assert.ok(entry.guards.every(g => g.expectedVersion.source === 'applyCommit'));
});
test('old manifests and altered or removed pair guards are refused', () => {
  const f = slotOnlyFixture(), m = plan(offlineSnapshot(f), {}, key);
  for (const change of [copy => { copy.format = 1; }, copy => { copy.entries[0].guards.pop(); }, copy => { copy.entries[0].guards[0].expectedVersion.source = 'applyCommit'; }]) {
    const copy = structuredClone(m); change(copy);
    assert.throws(() => validateManifest(copy, key), /MANIFEST_/);
  }
});
test('unapproved legacy timestamp/dateString is ambiguous', () => {
  const m = plan(offlineSnapshot(), {}, key);
  assert.ok(codes(m).includes('APPOINTMENT_EVIDENCE_REQUIRED'));
});
test('canonical pair needs no changes; terminal bookings remain historical', () => {
  const f = fixture({ canonical: true });
  assert.equal(plan(offlineSnapshot(f), {}, key).entries.length, 0);
  f.docs[f.bookings[0]].status = 'completed';
  const m = plan(offlineSnapshot(f), {}, key);
  assert.deepEqual(codes(m), []); assert.equal(m.entries.length, 0);
  assert.ok(m.issues.some(i => i.code === 'COMPLETED_RESERVATION_RETAINED'));
});
test('cancelled reservation and orphan are blocked', () => {
  const f = fixture(); f.docs[f.bookings[0]].status = 'cancelled';
  assert.ok(codes(plan(offlineSnapshot(f), {}, key)).includes('CANCELLED_BOOKING_STILL_RESERVED'));
  delete f.docs[f.bookings[0]];
  assert.ok(codes(plan(offlineSnapshot(f), {}, key)).includes('ORPHAN_RESERVED_SLOT'));
});
test('available legacy slot needs its own appointment evidence', () => {
  const f = fixture(); delete f.docs[f.bookings[0]];
  Object.assign(f.docs[f.slots[0]], { reserved: false, status: 'available', bookingId: '' });
  assert.ok(codes(plan(offlineSnapshot(f), {}, key)).includes('APPOINTMENT_EVIDENCE_REQUIRED'));
  f.decisions[f.slots[0]] = f.decisions[f.bookings[0]];
  const m = plan(offlineSnapshot(f), f.decisions, key);
  assert.deepEqual(codes(m), []); assert.equal(m.entries.length, 1);
  assert.equal(decode(m.entries[0].operations[0].after).isReserved, false);
});
test('conflicting appointments are never assigned an arbitrary winner', () => {
  const f = fixture({ count: 2, missing: true });
  f.decisions[f.bookings[1]].dateString = f.decisions[f.bookings[0]].dateString;
  const m = plan(offlineSnapshot(f), f.decisions, key);
  assert.ok(codes(m).includes('CONFLICTING_CANONICAL_TARGET'));
});
test('existing target collision and multiple linked reservations block migration', () => {
  const f = fixture({ oldId: true });
  const target = identity('garage_apex', '2031-08-06', '11:00 AM').id;
  f.docs[`slots/${target}`] = { ...f.docs[f.slots[0]], id: target, bookingId: 'unknown' };
  assert.ok(codes(plan(offlineSnapshot(f), f.decisions, key)).includes('CANONICAL_TARGET_ALREADY_EXISTS'));
  f.docs[`slots/${target}`].bookingId = f.docs[f.bookings[0]].id;
  assert.ok(codes(plan(offlineSnapshot(f), f.decisions, key)).includes('MULTIPLE_SLOTS_FOR_BOOKING'));
});
test('different legacy ID requires explicit retirement; never deletes', () => {
  const f = fixture({ oldId: true }); delete f.decisions[f.bookings[0]].retireLegacySlot;
  assert.ok(codes(plan(offlineSnapshot(f), f.decisions, key)).includes('EXPLICIT_RETIREMENT_APPROVAL_REQUIRED'));
  f.decisions[f.bookings[0]].retireLegacySlot = true;
  const m = plan(offlineSnapshot(f), f.decisions, key);
  assert.deepEqual(codes(m), []);
  assert.equal(decode(m.entries[0].operations.find(op => op.path === f.slots[0]).after).status, 'retired');
});
test('missing slot and foreign ownership require explicit resolution', () => {
  const f = fixture({ missing: true }); delete f.decisions[f.bookings[0]].createMissingSlot;
  assert.ok(codes(plan(offlineSnapshot(f), f.decisions, key)).includes('MISSING_SLOT_APPROVAL_REQUIRED'));
  f.docs['vehicles/vehicle_fixture'].ownerId = 'different';
  assert.ok(codes(plan(offlineSnapshot(f), f.decisions, key)).includes('OWNERSHIP_OR_ROLE_REVIEW_REQUIRED'));
});
test('conflicting aliases and public PII block conversion', () => {
  const f = fixture(); f.docs[f.slots[0]].isReserved = false;
  assert.ok(codes(plan(offlineSnapshot(f), f.decisions, key)).includes('CONFLICTING_RESERVED_ALIASES'));
  f.docs[f.slots[0]].customerPhone = 'synthetic';
  assert.ok(codes(plan(offlineSnapshot(f), f.decisions, key)).includes('UNEXPECTED_PUBLIC_SLOT_FIELDS'));
});
test('enabled policy blocks planning; no plan operation touches the policy', () => {
  const f = fixture(); f.policy.legacyMigrationComplete = true;
  const m = plan(offlineSnapshot(f), f.decisions, key);
  assert.ok(codes(m).includes('ROLLOUT_MUST_BE_DISABLED'));
  assert.ok(m.entries.every(e => e.operations.every(op => /^(slots|bookings)\//.test(op.path))));
});
test('encryption authenticates private originals, summary contains no names or IDs', () => {
  const m = plan(offlineSnapshot(), fixture().decisions, key), encrypted = seal(m, key);
  assert.deepEqual(unseal(encrypted, key), m);
  assert.ok(!stable(encrypted).includes('DO_NOT_LOG_SYNTHETIC_PERSON'));
  assert.ok(!stable(summary(m)).includes('booking_fixture'));
  assert.throws(() => unseal(encrypted, keyFromHex('32'.repeat(32))));
  encrypted.tag = '00'.repeat(16); assert.throws(() => unseal(encrypted, key));
  const tampered = structuredClone(m); tampered.entries[0].operations[0].path = 'users/owner_fixture';
  assert.throws(() => validateManifest(tampered, key));
});
