import { test } from 'node:test';
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdtempSync, readFileSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { Emulator, HOST, ROOT, write } from '../../tools/reservation-migration/emulator.mjs';
import { decode, fields, identity, keyFromHex, POLICY, stable, unseal } from '../../tools/reservation-migration/plan.mjs';
import { dryRun, execute } from '../../tools/reservation-migration/execute.mjs';
import { bookingOnlyFixture, emulator, fixture, seed, slotOnlyFixture, TEST_KEY_HEX } from './migration-fixtures.mjs';

const key = keyFromHex(TEST_KEY_HEX);
const approval = m => ({ mode: 'apply', approveManifest: m.hash, maintenanceEvidence: 'synthetic-freeze-ticket', operator: 'synthetic-operator' });
const rollback = m => ({ ...approval(m), mode: 'rollback' });
async function reset() {
  const response = await fetch(`${HOST}/emulator/v1/projects/demo-autocare-migration/databases/(default)/documents`, { method: 'DELETE', redirect: 'error' });
  assert.ok(response.ok, 'Local synthetic reset failed');
}
async function setup(options) {
  return setupFixture(fixture(options));
}
async function setupFixture(f) {
  await reset(); const db = emulator(); await seed(db, f);
  return { db, f, m: await dryRun(db, f.decisions, key) };
}
async function adminSet(db, path, data) { return db.request(`/v1/${ROOT}:commit`, { writes: [{ update: { name: `${ROOT}/${path}`, fields: fields(data) } }] }); }
function token(uid) {
  const b64 = v => Buffer.from(JSON.stringify(v)).toString('base64url'), now = Math.floor(Date.now() / 1000);
  return `${b64({ alg: 'none', typ: 'JWT' })}.${b64({ sub: uid, user_id: uid, aud: 'demo-autocare-migration', iss: 'https://securetoken.google.com/demo-autocare-migration', iat: now, exp: now + 3600, firebase: { sign_in_provider: 'custom', identities: {} } })}.`;
}
async function client(path, method = 'GET', body, uid = 'customer_fixture') {
  return fetch(`${HOST}/v1/${ROOT}${path}`, { method, redirect: 'error', headers: { authorization: `Bearer ${token(uid)}`, 'content-type': 'application/json' }, ...(body ? { body: JSON.stringify(body) } : {}) });
}
async function ownerConfirm(path) {
  const response = await client(':commit', 'POST', { writes: [{ update: { name: `${ROOT}/${path}`, fields: fields({ status: 'confirmed', updatedAt: 999 }) }, updateMask: { fieldPaths: ['status', 'updatedAt'] } }] }, 'owner_fixture');
  assert.equal(response.status, 200, 'Owner confirmation must be authorized while booking creation remains disabled');
}

test('isolated reservation migration emulator scenarios', async t => {
  t.afterEach(async () => {
    const policy = await emulator().get(POLICY);
    if (policy) assert.equal(decode(policy.fields).legacyMigrationComplete, false, 'Every scenario must leave the actual rollout gate disabled');
  });
  const rules = await fetch(`${HOST}/emulator/v1/projects/demo-autocare-migration:securityRules`, { method: 'PUT', redirect: 'error', headers: { authorization: 'Bearer owner', 'content-type': 'application/json' }, body: JSON.stringify({ rules: { files: [{ name: 'firestore.rules', content: readFileSync(new URL('../../firestore.rules', import.meta.url), 'utf8') }] } }) });
  assert.ok(rules.ok, 'Current rules must compile');

  await t.test('dry-run and default executor make no document or audit writes', async () => {
    const { db, f } = await setup(); const before = await db.readSnapshot();
    const m = await dryRun(db, f.decisions, key); await execute(db, m, key);
    assert.deepEqual(await db.readSnapshot(), before);
    assert.deepEqual(await db.journal(m.hash), {});
  });
  await t.test('apply requires matching manifest approval and maintenance acknowledgment', async () => {
    const { db, m } = await setup();
    await assert.rejects(execute(db, m, key, { mode: 'apply' }), /EXPLICIT_REVIEW/);
    await assert.rejects(execute(db, m, key, { ...approval(m), approveManifest: 'wrong' }), /EXPLICIT_REVIEW/);
    assert.deepEqual(await db.journal(m.hash), {});
  });
  await t.test('legacy pair normalized in one atomic unit; originals remain in encrypted audit', async () => {
    const { db, f, m } = await setup(); const before = await db.readSnapshot();
    const result = await execute(db, m, key, approval(m)); assert.equal(result.completed, 1);
    const after = await db.readSnapshot(), booking = decode(after.docs[f.bookings[0]].fields), slot = after.docs[f.slots[0]];
    assert.equal(booking.id, f.docs[f.bookings[0]].id); assert.equal(booking.slotId, f.slots[0].slice(6));
    assert.equal(booking.scheduledDate, identity('garage_apex', '2031-08-06', '11:00 AM').scheduledDate);
    assert.equal(decode(slot.fields).reservationVersion, 2); assert.ok(!('reserved' in slot.fields));
    const journal = await db.journal(m.hash), record = Object.values(journal)[0];
    assert.equal(record.updateTime, slot.updateTime);
    assert.equal(record.updateTime, after.docs[f.bookings[0]].updateTime);
    assert.ok(!stable(journal).includes('DO_NOT_LOG_SYNTHETIC_PERSON'));
    const audit = unseal(JSON.parse(decode(record.fields).payload), key);
    assert.deepEqual(audit.entry.operations.find(op => op.path === f.bookings[0]).before, before.docs[f.bookings[0]]);
    assert.equal(decode(after.policy.fields).legacyMigrationComplete, false);
  });
  await t.test('canonical pair rerun is unchanged', async () => {
    const { db, m } = await setup({ canonical: true }); const before = await db.readSnapshot();
    assert.equal((await execute(db, m, key, approval(m))).completed, 0);
    assert.deepEqual(await db.readSnapshot(), before);
  });
  await t.test('regression: slot-only migration refuses rollback after its unchanged booking is confirmed', async () => {
    const { db, f, m } = await setupFixture(slotOnlyFixture()), originalBooking = await db.get(f.bookings[0]);
    assert.deepEqual(m.entries[0].operations.map(op => op.path), f.slots);
    await execute(db, m, key, approval(m));
    assert.deepEqual(await db.get(f.bookings[0]), originalBooking, 'No booking write, including a no-op write, may be introduced');
    const receipt = Object.values(await db.journal(m.hash))[0];
    const guard = unseal(JSON.parse(decode(receipt.fields).payload), key).entry.guards.find(g => g.path === f.bookings[0]);
    assert.equal(guard.expectedVersion.updateTime, originalBooking.updateTime);
    assert.notEqual(receipt.updateTime, originalBooking.updateTime);
    await ownerConfirm(f.bookings[0]);
    const before = await db.readSnapshot(), journal = await db.journal(m.hash);
    await assert.rejects(execute(db, m, key, rollback(m)), /POST_MIGRATION_ACTIVITY/);
    assert.deepEqual(await db.readSnapshot(), before);
    assert.deepEqual(await db.journal(m.hash), journal);
    assert.ok(!('reserved' in (await db.get(f.slots[0])).fields), 'Refusal must leave the normalized slot intact');
    assert.equal(decode((await db.get(POLICY)).fields).legacyMigrationComplete, false);
  });
  await t.test('normal slot-only rollback restores the slot without writing its unchanged booking', async () => {
    const { db, f, m } = await setupFixture(slotOnlyFixture());
    const before = await db.readSnapshot();
    await execute(db, m, key, approval(m));
    await execute(db, m, key, rollback(m));
    assert.deepEqual(await db.get(f.bookings[0]), before.docs[f.bookings[0]]);
    assert.deepEqual((await db.get(f.slots[0])).fields, before.docs[f.slots[0]].fields);
    const restored = await db.readSnapshot();
    assert.equal((await execute(db, m, key, rollback(m))).completed, 0);
    assert.deepEqual(await db.readSnapshot(), restored);
  });
  await t.test('booking-only rollback guards unchanged slot ownership and refuses without partial restoration', async () => {
    const { db, f, m } = await setupFixture(bookingOnlyFixture()), originalSlot = await db.get(f.slots[0]);
    assert.deepEqual(m.entries[0].operations.map(op => op.path), f.bookings);
    await execute(db, m, key, approval(m));
    assert.deepEqual(await db.get(f.slots[0]), originalSlot);
    await adminSet(db, f.slots[0], { ...decode(originalSlot.fields), bookingId: 'different_customer_booking' });
    const before = await db.readSnapshot(), journal = await db.journal(m.hash);
    await assert.rejects(execute(db, m, key, rollback(m)), /POST_MIGRATION_ACTIVITY/);
    assert.deepEqual(await db.readSnapshot(), before);
    assert.deepEqual(await db.journal(m.hash), journal);
  });
  await t.test('normal booking-only rollback leaves the unchanged slot and its version intact', async () => {
    const { db, f, m } = await setupFixture(bookingOnlyFixture()), before = await db.readSnapshot();
    await execute(db, m, key, approval(m));
    await execute(db, m, key, rollback(m));
    assert.deepEqual(await db.get(f.slots[0]), before.docs[f.slots[0]]);
    assert.deepEqual((await db.get(f.bookings[0])).fields, before.docs[f.bookings[0]].fields);
  });
  await t.test('unchanged booking guard detects change-and-restore even when ownership and fields match again', async () => {
    const { db, f, m } = await setupFixture(slotOnlyFixture());
    await execute(db, m, key, approval(m));
    const b = decode((await db.get(f.bookings[0])).fields);
    await adminSet(db, f.bookings[0], { ...b, customerId: 'different_customer' });
    await adminSet(db, f.bookings[0], b);
    const before = await db.readSnapshot();
    await assert.rejects(execute(db, m, key, rollback(m)), /POST_MIGRATION_ACTIVITY/);
    assert.deepEqual(await db.readSnapshot(), before);
  });
  await t.test('pair guards are rechecked after rollback preflight and before its transaction writes', async () => {
    const { db, f, m } = await setupFixture(slotOnlyFixture());
    await execute(db, m, key, approval(m));
    let inject = true;
    class ConfirmAfterPreflight extends Emulator {
      async abort(transaction) {
        await super.abort(transaction);
        if (inject) { inject = false; await ownerConfirm(f.bookings[0]); }
      }
    }
    const changed = new ConfirmAfterPreflight({ project: 'demo-autocare-migration', database: '(default)' });
    const normalized = await db.get(f.slots[0]), journal = await db.journal(m.hash);
    await assert.rejects(execute(changed, m, key, rollback(m)), /POST_MIGRATION_ACTIVITY/);
    assert.deepEqual(await db.get(f.slots[0]), normalized);
    assert.deepEqual(await db.journal(m.hash), journal);
  });
  await t.test('slot-only apply and rollback resume with unchanged partner versions intact', async () => {
    const { db, f, m } = await setupFixture(slotOnlyFixture({ count: 2 }));
    const bookings = await Promise.all(f.bookings.map(path => db.get(path)));
    assert.equal((await execute(db, m, key, { ...approval(m), maxUnits: 1 })).completed, 1);
    assert.equal((await execute(db, m, key, approval(m))).completed, 1);
    assert.equal((await execute(db, m, key, { ...rollback(m), maxUnits: 1 })).completed, 1);
    const resumed = await execute(db, m, key, rollback(m));
    assert.equal(resumed.completed, 1); assert.equal(resumed.skipped, 1);
    assert.deepEqual(await Promise.all(f.bookings.map(path => db.get(path))), bookings);
  });
  for (const state of ['applied', 'rolledBack']) await t.test(`resumed rollback rechecks unchanged partners in ${state} units`, async () => {
    const { db, m } = await setupFixture(slotOnlyFixture({ count: 2 }));
    await execute(db, m, key, approval(m));
    await execute(db, m, key, { ...rollback(m), maxUnits: 1 });
    const journal = await db.journal(m.hash);
    const receipt = Object.values(journal).find(record => decode(record.fields).state === state);
    const entry = unseal(JSON.parse(decode(receipt.fields).payload), key).entry;
    await ownerConfirm(entry.guards.find(g => g.path.startsWith('bookings/')).path);
    const before = await db.readSnapshot();
    await assert.rejects(execute(db, m, key, rollback(m)), /POST_MIGRATION_ACTIVITY/);
    assert.deepEqual(await db.readSnapshot(), before);
    assert.deepEqual(await db.journal(m.hash), journal);
  });
  await t.test('an existing unrelated record may change after migration without being overwritten on rollback', async () => {
    const f = slotOnlyFixture(); f.docs['users/unrelated_fixture'] = { role: 'CUSTOMER', name: 'synthetic-before' };
    const { db, m } = await setupFixture(f);
    await execute(db, m, key, approval(m));
    await adminSet(db, 'users/unrelated_fixture', { role: 'CUSTOMER', name: 'synthetic-after' });
    const unrelated = await db.get('users/unrelated_fixture');
    assert.equal((await execute(db, m, key, rollback(m))).completed, 1);
    assert.deepEqual(await db.get('users/unrelated_fixture'), unrelated);
  });
  await t.test('ambiguous legacy dates block the entire manifest without writes', async () => {
    const { db } = await setup(); const m = await dryRun(db, {}, key), before = await db.readSnapshot();
    await assert.rejects(execute(db, m, key, approval(m)), /UNRESOLVED_BLOCKING/);
    assert.deepEqual(await db.readSnapshot(), before);
  });
  await t.test('conflicting appointments block both customers', async () => {
    await reset(); const db = emulator(), f = fixture({ count: 2, missing: true });
    f.decisions[f.bookings[1]].dateString = f.decisions[f.bookings[0]].dateString;
    await seed(db, f); const m = await dryRun(db, f.decisions, key);
    assert.ok(m.issues.some(i => i.code === 'CONFLICTING_CANONICAL_TARGET'));
    await assert.rejects(execute(db, m, key, approval(m)), /UNRESOLVED_BLOCKING/);
    assert.deepEqual(await db.journal(m.hash), {});
  });
  await t.test('orphan reserved slot blocks migration', async () => {
    await reset(); const db = emulator(), f = fixture(); delete f.docs[f.bookings[0]];
    await seed(db, f); const m = await dryRun(db, {}, key);
    assert.ok(m.issues.some(i => i.code === 'ORPHAN_RESERVED_SLOT'));
    await assert.rejects(execute(db, m, key, approval(m)), /UNRESOLVED_BLOCKING/);
  });
  await t.test('an existing canonical target ID is not overwritten by a legacy rename', async () => {
    await reset(); const db = emulator(), f = fixture({ oldId: true });
    const i = identity('garage_apex', '2031-08-06', '11:00 AM');
    f.docs[`slots/${i.id}`] = { ...i, isReserved: true, bookingId: 'another_customer_booking', status: 'reserved', updatedAt: 100 };
    await seed(db, f); const m = await dryRun(db, f.decisions, key), before = await db.readSnapshot();
    assert.ok(m.issues.some(issue => issue.code === 'CANONICAL_TARGET_ALREADY_EXISTS'));
    await assert.rejects(execute(db, m, key, approval(m)), /UNRESOLVED_BLOCKING/);
    assert.deepEqual(await db.readSnapshot(), before);
  });
  await t.test('legacy available slot can be normalized with explicit evidence', async () => {
    await reset(); const db = emulator(), f = fixture(); delete f.docs[f.bookings[0]];
    Object.assign(f.docs[f.slots[0]], { reserved: false, status: 'available', bookingId: '' });
    f.decisions[f.slots[0]] = f.decisions[f.bookings[0]];
    await seed(db, f); const m = await dryRun(db, f.decisions, key);
    await execute(db, m, key, approval(m));
    const slot = decode((await db.get(f.slots[0])).fields);
    assert.equal(slot.isReserved, false); assert.equal(slot.reservationVersion, 2); assert.equal(slot.bookingId, '');
  });
  await t.test('completed historical reservation is retained; cancelled history is not guessed', async () => {
    await reset(); const db = emulator(), f = fixture(); f.docs[f.bookings[0]].status = 'completed';
    await seed(db, f); const m = await dryRun(db, {}, key), before = await db.readSnapshot();
    assert.equal((await execute(db, m, key, approval(m))).completed, 0);
    assert.deepEqual(await db.readSnapshot(), before);
  });
  await t.test('changed dependency invalidates the manifest before any writes', async () => {
    const { db, m } = await setup();
    await adminSet(db, 'vehicles/vehicle_fixture', { id: 'vehicle_fixture', ownerId: 'another_customer' });
    await assert.rejects(execute(db, m, key, approval(m)), /INVENTORY_OR_VERSION_CHANGED/);
    assert.deepEqual(await db.journal(m.hash), {});
  });
  await t.test('Firestore itself rejects stale updateTime preconditions', async () => {
    const { db, f, m } = await setup(); const op = m.entries[0].operations.find(o => o.path === f.bookings[0]);
    await adminSet(db, f.bookings[0], { ...f.docs[f.bookings[0]], notes: 'changed after plan' });
    await assert.rejects(db.request(`/v1/${ROOT}:commit`, { writes: [write(op.path, op.after, op.before)] }), error => error.code === 'FAILED_PRECONDITION');
    assert.equal(decode((await db.get(op.path)).fields).notes, 'changed after plan');
  });
  await t.test('a failed transaction version precondition leaves every document and audit unchanged', async () => {
    const { db, m } = await setup(), before = await db.readSnapshot();
    class StaleCommit extends Emulator {
      async commit(writes, transaction) {
        const stale = structuredClone(writes);
        stale[0].currentDocument = { updateTime: '2020-01-01T00:00:00Z' };
        return super.commit(stale, transaction);
      }
    }
    const stale = new StaleCommit({ project: 'demo-autocare-migration', database: '(default)' });
    await assert.rejects(execute(stale, m, key, approval(m)), error => error.code === 'FAILED_PRECONDITION');
    assert.deepEqual(await db.readSnapshot(), before);
    assert.deepEqual(await db.journal(m.hash), {});
  });
  await t.test('interrupted application resumes and reruns idempotently', async () => {
    const { db, m } = await setup({ count: 2 });
    assert.equal((await execute(db, m, key, { ...approval(m), maxUnits: 1 })).completed, 1);
    assert.equal(Object.keys(await db.journal(m.hash)).length, 1);
    const resumed = await execute(db, m, key, approval(m)); assert.equal(resumed.completed, 1); assert.equal(resumed.skipped, 1);
    const rerun = await execute(db, m, key, approval(m)); assert.equal(rerun.completed, 0); assert.equal(rerun.skipped, 2);
  });
  await t.test('lost commit acknowledgment resumes from the atomic journal', async () => {
    const { db, m } = await setup();
    class LoseAck extends Emulator { async commit(...args) { await super.commit(...args); throw new Error('SIMULATED_LOST_ACK'); } }
    const unreliable = new LoseAck({ project: 'demo-autocare-migration', database: '(default)' });
    await assert.rejects(execute(unreliable, m, key, approval(m)), /SIMULATED_LOST_ACK/);
    const resumed = await execute(db, m, key, approval(m)); assert.equal(resumed.completed, 0); assert.equal(resumed.skipped, 1);
  });
  await t.test('a new record during an interrupted migration forces replanning', async () => {
    const { db, m } = await setup({ count: 2 });
    await execute(db, m, key, { ...approval(m), maxUnits: 1 });
    await adminSet(db, 'bookings/new_during_maintenance', { id: 'new_during_maintenance', status: 'cancelled' });
    await assert.rejects(execute(db, m, key, approval(m)), /INVENTORY_OR_VERSION_CHANGED/);
    assert.equal(Object.keys(await db.journal(m.hash)).length, 1);
  });
  await t.test('approved ID retirement retains originals and never deletes legacy documents', async () => {
    const { db, f, m } = await setup({ oldId: true });
    await execute(db, m, key, approval(m));
    const legacy = decode((await db.get(f.slots[0])).fields), booking = decode((await db.get(f.bookings[0])).fields);
    assert.equal(legacy.status, 'retired'); assert.equal(legacy.isReserved, false);
    assert.notEqual(`slots/${booking.slotId}`, f.slots[0]);
    assert.equal(decode((await db.get(`slots/${booking.slotId}`)).fields).bookingId, booking.id);
  });
  await t.test('explicit missing-slot approval creates one canonical reservation', async () => {
    const { db, f, m } = await setup({ missing: true });
    await execute(db, m, key, approval(m));
    const b = decode((await db.get(f.bookings[0])).fields);
    assert.equal(decode((await db.get(`slots/${b.slotId}`)).fields).bookingId, b.id);
  });
  await t.test('rollback restores modified originals, quarantines created slots, preserves later unrelated records', async () => {
    const { db, f, m } = await setup({ oldId: true }); const before = await db.readSnapshot();
    await execute(db, m, key, approval(m));
    const target = `slots/${decode((await db.get(f.bookings[0])).fields).slotId}`;
    const originalReceipt = Object.values(await db.journal(m.hash))[0];
    await adminSet(db, 'bookings/post_cutover', { id: 'post_cutover', status: 'cancelled', notes: 'later unrelated history' });
    const laterIdentity = identity('garage_apex', '2035-01-15', '10:00 AM');
    await adminSet(db, `slots/${laterIdentity.id}`, { ...laterIdentity, bookingId: 'later_booking', isReserved: true, status: 'reserved', updatedAt: 123 });
    const later = await db.get('bookings/post_cutover');
    const laterSlot = await db.get(`slots/${laterIdentity.id}`);
    const result = await execute(db, m, key, rollback(m)); assert.equal(result.completed, 1); assert.equal(result.reconciliationRequired, true);
    assert.deepEqual((await db.get(f.bookings[0])).fields, before.docs[f.bookings[0]].fields);
    assert.deepEqual((await db.get(f.slots[0])).fields, before.docs[f.slots[0]].fields);
    assert.equal(decode((await db.get(target)).fields).status, 'retired');
    assert.deepEqual(await db.get('bookings/post_cutover'), later);
    assert.deepEqual(await db.get(`slots/${laterIdentity.id}`), laterSlot);
    const recoveredReceipt = Object.values(await db.journal(m.hash))[0];
    assert.equal(decode(recoveredReceipt.fields).payload, decode(originalReceipt.fields).payload);
    assert.equal(unseal(JSON.parse(decode(recoveredReceipt.fields).recoveryPayload), key).appliedVersion, originalReceipt.updateTime);
    assert.equal((await execute(db, m, key, rollback(m))).completed, 0);
    await assert.rejects(execute(db, m, key, approval(m)), /ROLLED_BACK_MANIFEST/);
  });
  await t.test('rollback blocks when migrated records have post-cutover activity', async () => {
    const { db, f, m } = await setup(); await execute(db, m, key, approval(m));
    const b = decode((await db.get(f.bookings[0])).fields);
    await adminSet(db, f.bookings[0], { ...b, status: 'confirmed', updatedAt: 999 });
    const before = await db.readSnapshot();
    await assert.rejects(execute(db, m, key, rollback(m)), /POST_MIGRATION_ACTIVITY/);
    assert.deepEqual(await db.readSnapshot(), before);
  });
  await t.test('rollback detects ABA changes even if the final field values match', async () => {
    const { db, f, m } = await setup(); await execute(db, m, key, approval(m));
    const b = decode((await db.get(f.bookings[0])).fields);
    await adminSet(db, f.bookings[0], { ...b, notes: 'temporary edit' });
    await adminSet(db, f.bookings[0], b);
    await assert.rejects(execute(db, m, key, rollback(m)), /POST_MIGRATION_ACTIVITY/);
  });
  await t.test('rollback blocks a changed ownership dependency', async () => {
    const { db, m } = await setup(); await execute(db, m, key, approval(m));
    await adminSet(db, 'garages/garage_apex', { id: 'garage_apex', ownerId: 'different_owner' });
    await assert.rejects(execute(db, m, key, rollback(m)), /POST_MIGRATION_DEPENDENCY/);
  });
  await t.test('interrupted rollback resumes without restoring twice', async () => {
    const { db, m } = await setup({ count: 2 }); await execute(db, m, key, approval(m));
    assert.equal((await execute(db, m, key, { ...rollback(m), maxUnits: 1 })).completed, 1);
    const resumed = await execute(db, m, key, rollback(m)); assert.equal(resumed.completed, 1); assert.equal(resumed.skipped, 1);
  });
  await t.test('an enabled-policy read blocks apply and rollback without ever enabling the actual gate', async () => {
    const { db, m } = await setup(); await execute(db, m, key, approval(m));
    class EnabledPolicyRead extends Emulator {
      async snapshot(transaction) {
        const snapshot = await super.snapshot(transaction);
        return { ...snapshot, policy: { ...snapshot.policy, fields: fields({ version: 2, timeZoneId: 'Asia/Kolkata', legacyMigrationComplete: true }) } };
      }
    }
    const simulated = new EnabledPolicyRead({ project: 'demo-autocare-migration', database: '(default)' });
    await assert.rejects(execute(simulated, m, key, approval(m)), /ROLLOUT_MUST_BE_DISABLED/);
    await assert.rejects(execute(simulated, m, key, rollback(m)), /ROLLOUT_MUST_BE_DISABLED/);
    assert.equal(decode((await db.get(POLICY)).fields).legacyMigrationComplete, false);
  });
  await t.test('normal clients cannot enable policy or read the encrypted audit', async () => {
    const { db, m } = await setup(); await execute(db, m, key, approval(m));
    const policyWrite = { update: { name: `${ROOT}/${POLICY}`, fields: fields({ version: 2, timeZoneId: 'Asia/Kolkata', legacyMigrationComplete: true }) } };
    assert.equal((await client(':commit', 'POST', { writes: [policyWrite] })).status, 403);
    assert.equal((await client(`/reservationMigrationRuns/${m.hash}/entries/${m.entries[0].id}`)).status, 403);
    assert.equal(decode((await db.get(POLICY)).fields).legacyMigrationComplete, false);
  });
  await t.test('gate stays disabled and blocks a valid new pair after migration', async () => {
    const { db, f, m } = await setup(); await execute(db, m, key, approval(m));
    const i = identity('garage_apex', '2035-08-06', '11:00 AM'), id = 'post_review_fixture';
    const b = { ...f.docs[f.bookings[0]], id, slotId: i.id, dateString: i.dateString, scheduledDate: i.scheduledDate, timeZoneId: i.timeZoneId, reservationVersion: 2 };
    const s = { ...i, bookingId: id, isReserved: true, status: 'reserved', updatedAt: 100 };
    const writes = [{ update: { name: `${ROOT}/bookings/${id}`, fields: fields(b) } }, { update: { name: `${ROOT}/slots/${i.id}`, fields: fields(s) } }];
    assert.equal((await client(':commit', 'POST', { writes })).status, 403);
    assert.equal(await db.get(`bookings/${id}`), null);
    assert.equal(decode((await db.get(POLICY)).fields).legacyMigrationComplete, false);
  });
  await t.test('CLI defaults to dry-run, encrypts output, refuses to overwrite artifacts', async () => {
    const { db, f } = await setup(); const before = await db.readSnapshot();
    const dir = mkdtempSync(resolve('.gradle', 'migration-test-'));
    const decisionFile = resolve(dir, 'synthetic-decisions.json'), output = resolve(dir, 'manifest.enc.json');
    writeFileSync(decisionFile, stable(f.decisions));
    const args = ['tools/reservation-migration/cli.mjs', '--project', 'demo-autocare-migration', '--database', '(default)', '--decisions', decisionFile, '--output', output];
    const child = spawnSync(process.execPath, args, { env: { ...process.env, AUTOCARE_MIGRATION_KEY: TEST_KEY_HEX, FIRESTORE_EMULATOR_HOST: '127.0.0.1:8788' }, encoding: 'utf8' });
    assert.equal(child.status, 0, child.stderr); const report = JSON.parse(child.stdout);
    assert.equal(report.blockers, 0); assert.equal(report.units, 1);
    assert.ok(!readFileSync(output, 'utf8').includes('DO_NOT_LOG_SYNTHETIC_PERSON'));
    assert.deepEqual(await db.readSnapshot(), before);
    const duplicate = spawnSync(process.execPath, args, { env: { ...process.env, AUTOCARE_MIGRATION_KEY: TEST_KEY_HEX }, encoding: 'utf8' });
    assert.equal(duplicate.status, 1);
    const common = ['tools/reservation-migration/cli.mjs', '--project', 'demo-autocare-migration', '--database', '(default)', '--manifest', output, '--approve-manifest', report.manifestHash, '--maintenance-evidence', 'synthetic-maintenance', '--operator', 'synthetic-operator'];
    const environment = { ...process.env, AUTOCARE_MIGRATION_KEY: TEST_KEY_HEX, FIRESTORE_EMULATOR_HOST: '127.0.0.1:8788' };
    const applied = spawnSync(process.execPath, [...common, '--mode', 'apply', '--max-units', '1'], { env: environment, encoding: 'utf8' });
    assert.equal(applied.status, 0, applied.stderr); assert.equal(JSON.parse(applied.stdout).completed, 1);
    const undone = spawnSync(process.execPath, [...common, '--mode', 'rollback'], { env: environment, encoding: 'utf8' });
    assert.equal(undone.status, 0, undone.stderr); assert.equal(JSON.parse(undone.stdout).completed, 1);
  });
});
