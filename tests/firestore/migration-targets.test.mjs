// Offline unit tests only. No emulator is contacted, started, seeded or reset.
import { after, test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';
import { spawnSync } from 'node:child_process';
import { DEFAULT_TARGET, PERSON1_TARGET, TARGETS, resolveTarget } from '../../tools/reservation-migration/targets.mjs';
import { Emulator, HOST, ROOT, write } from '../../tools/reservation-migration/emulator.mjs';
import { keyFromHex, plan, seal, stable, validateManifest } from '../../tools/reservation-migration/plan.mjs';
import { dryRun, execute } from '../../tools/reservation-migration/execute.mjs';
import { fixture, fixtureWrites, offlineSnapshot, slotOnlyFixture, TEST_KEY_HEX } from './migration-fixtures.mjs';

const key = keyFromHex(TEST_KEY_HEX), realFetch = globalThis.fetch;
globalThis.fetch = async () => { throw new Error('UNEXPECTED_NETWORK_ACCESS'); };
after(() => { globalThis.fetch = realFetch; });

test('exactly two immutable local target tuples; original defaults are unchanged', () => {
  assert.equal(TARGETS.length, 2);
  assert.equal(HOST, 'http://127.0.0.1:8788'); assert.equal(ROOT, DEFAULT_TARGET.root);
  assert.equal(PERSON1_TARGET.host, 'http://127.0.0.1:8790');
  assert.equal(PERSON1_TARGET.project, 'demo-autocare-person1-migration');
  assert.ok(Object.isFrozen(TARGETS));
  for (const target of TARGETS) {
    assert.ok(Object.isFrozen(target)); assert.equal(target.database, '(default)');
    assert.equal(resolveTarget({ project: target.project, database: target.database }), target);
    assert.equal(new Emulator(target).target, target);
    assert.throws(() => { new Emulator(target).target = DEFAULT_TARGET; }, TypeError);
  }
});
test('mixed approved projects/ports, hosts and document roots are rejected', () => {
  for (const a of TARGETS) for (const b of TARGETS.filter(t => t !== a)) {
    for (const mismatch of [{ emulatorHost: b.emulatorHost }, { host: b.host }, { root: b.root }]) {
      assert.throws(() => new Emulator({ ...a, ...mismatch }), /ENVIRONMENT_REFUSED/);
      const snapshot = { ...offlineSnapshot(fixture(), a), ...mismatch };
      assert.throws(() => plan(snapshot, {}, key), /ENVIRONMENT_REFUSED/);
    }
  }
});
test('arbitrary projects, databases, schemes, addresses and ports are refused', () => {
  for (const target of TARGETS) for (const mismatch of [
    { project: 'production' }, { project: 'demo-unapproved' }, { database: 'other' },
    { emulatorHost: 'localhost:8790' }, { emulatorHost: '0.0.0.0:8790' },
    { emulatorHost: '127.0.0.1:8791' }, { emulatorHost: 'https://firestore.googleapis.com' },
    { emulatorHost: '127.0.0.1:8790@evil.example' }, { emulatorHost: null }, { host: 'https://firestore.googleapis.com' }
  ]) assert.throws(() => new Emulator({ ...target, ...mismatch }), /ENVIRONMENT_REFUSED/);
  assert.throws(() => new Emulator(), /ENVIRONMENT_REFUSED/);
  assert.throws(() => new Emulator({ project: DEFAULT_TARGET.project }), /ENVIRONMENT_REFUSED/);
});
for (const target of TARGETS) {
  test(`planner and validation bind full tuple: ${target.project}`, () => {
    const f = slotOnlyFixture(), m = plan(offlineSnapshot(f, target), f.decisions, key);
    assert.equal(m.format, 2); assert.equal(m.project, target.project); assert.equal(m.database, target.database);
    assert.equal(m.emulatorHost, target.emulatorHost);
    assert.equal(m.issues.filter(i => i.blocking).length, 0);
    assert.equal(m.entries[0].guards.length, 2); // Pair guard is retained.
    validateManifest(m, key, target);
    const copy = structuredClone(m); copy.emulatorHost = '127.0.0.1:9999';
    assert.throws(() => validateManifest(copy, key), /ENVIRONMENT_REFUSED/);
  });
  test(`REST routes and snapshot identify selected tuple: ${target.project}`, async t => {
    const requests = [], db = new Emulator(target), f = fixture();
    const docs = offlineSnapshot(f, target).docs;
    t.mock.method(globalThis, 'fetch', async (url, options) => {
      const body = JSON.parse(options.body); requests.push({ url, options, body });
      assert.ok(url.startsWith(`${target.host}/v1/${target.root}`));
      assert.equal(options.redirect, 'error');
      if (url.endsWith(':runQuery')) {
        const collection = body.structuredQuery.from[0].collectionId;
        const rows = Object.entries(docs).filter(([p]) => p.startsWith(`${collection}/`)).map(([p, d]) => ({ document: { name: `${target.root}/${p}`, ...d } }));
        return { ok: true, json: async () => rows };
      }
      if (url.endsWith(':batchGet')) return { ok: true, json: async () => [{ missing: body.documents[0] }] };
      return { ok: true, json: async () => ({ transaction: 'offline-transaction' }) };
    });
    const snapshot = await db.readSnapshot();
    assert.equal(snapshot.project, target.project); assert.equal(snapshot.emulatorHost, target.emulatorHost);
    assert.deepEqual(snapshot.docs, docs);
    assert.equal(requests.filter(r => r.url.endsWith(':runQuery')).length, 6);
    assert.ok(requests.every(r => r.body.writes === undefined), 'Snapshot path may not submit a document write');
  });
  test(`write and fixture serialization use only selected root: ${target.project}`, () => {
    const f = fixture(), writes = fixtureWrites(f, target);
    assert.ok(writes.every(w => w.update.name.startsWith(`${target.root}/`) && w.currentDocument.exists === false));
    assert.ok(writes.some(w => w.update.name === `${target.root}/reservationPolicies/current` && w.update.fields.legacyMigrationComplete.booleanValue === false));
    const before = { updateTime: '2030-01-01T00:00:00Z' };
    assert.equal(write('bookings/synthetic', {}, before, target).update.name, `${target.root}/bookings/synthetic`);
    assert.deepEqual(write('bookings/synthetic', {}, before, target).currentDocument, before);
    assert.throws(() => write('reservationPolicies/current', {}, null, target), /WRITE_SCOPE_REFUSED/);
  });
  test(`adapter rejects foreign request/document roots before transport: ${target.project}`, async () => {
    const db = new Emulator(target), foreign = TARGETS.find(t => t !== target);
    await assert.rejects(db.request(`/v1/${foreign.root}:commit`, { writes: [] }), /INVALID_LOCAL_ENDPOINT/);
    await assert.rejects(db.request(`/v1/${target.root}:commit`, { writes: [{ update: { name: `${foreign.root}/bookings/synthetic`, fields: {} } }] }), /CROSS_TARGET_DOCUMENT/);
    await assert.rejects(db.request(`/v1/${target.root}:batchGet`, { documents: [`${foreign.root}/bookings/synthetic`] }), /CROSS_TARGET_DOCUMENT/);
  });
  test(`adapter rejects foreign snapshot documents: ${target.project}`, async t => {
    const foreign = TARGETS.find(t => t !== target), db = new Emulator(target);
    t.mock.method(globalThis, 'fetch', async () => ({ ok: true, json: async () => [{ document: { name: `${foreign.root}/bookings/synthetic`, fields: {} } }] }));
    await assert.rejects(db.query('bookings'), /CROSS_TARGET_DOCUMENT/);
    t.mock.method(globalThis, 'fetch', async () => ({ ok: true, json: async () => [{ found: { name: `${foreign.root}/bookings/synthetic`, fields: {} } }] }));
    await assert.rejects(db.get('bookings/synthetic'), /CROSS_TARGET_DOCUMENT/);
  });
  test(`encoded traversal, query and fragment suffixes cannot bypass namespace checks: ${target.project}`, async () => {
    const db = new Emulator(target);
    for (const suffix of [`/v1/${target.root}/%2e%2e/%2e%2e/other/doc`, `/v1/${target.root}:commit?project=other`, `/v1/${target.root}:commit#other`]) {
      await assert.rejects(db.request(suffix, {}), /INVALID_LOCAL_ENDPOINT/);
    }
  });
}
test('identical synthetic data on different targets produces different authenticated manifests', () => {
  const f = fixture(), manifests = TARGETS.map(t => plan(offlineSnapshot(f, t), f.decisions, key));
  assert.notEqual(manifests[0].hash, manifests[1].hash);
  for (let n = 0; n < 2; n++) assert.throws(() => validateManifest(manifests[n], key, TARGETS[1 - n]), /CROSS_TARGET/);
});
test('cross-target apply, rollback, resume and default execution are refused before any adapter reads', async () => {
  for (const source of TARGETS) {
    const foreign = TARGETS.find(t => t !== source), f = fixture(), m = plan(offlineSnapshot(f, source), f.decisions, key);
    const db = { target: foreign, begin: () => { throw new Error('ADAPTER_MUST_NOT_BE_READ'); } };
    for (const mode of [undefined, 'dry-run', 'apply', 'rollback']) {
      await assert.rejects(execute(db, m, key, { mode, approveManifest: m.hash, operator: 'synthetic', maintenanceEvidence: 'synthetic', maxUnits: 1 }), /CROSS_TARGET/);
    }
  }
});
test('offline dry-run rejects wrong snapshot identity and preserves planner defaults', async () => {
  const f = fixture();
  for (const target of TARGETS) {
    const foreign = TARGETS.find(t => t !== target);
    await assert.rejects(dryRun({ target, readSnapshot: async () => offlineSnapshot(f, foreign) }, f.decisions, key), /CROSS_TARGET/);
    const m = await dryRun({ target, readSnapshot: async () => offlineSnapshot(f, target) }, f.decisions, key);
    const result = await execute({ target }, m, key); // Default makes no adapter calls or writes.
    assert.equal(result.blockers, 0); assert.equal(result.writes, 2);
  }
});
test('CLI inspect accepts its own target and rejects cross-target manifests without network access', () => {
  const dir = mkdtempSync(resolve('.gradle', 'migration-target-unit-'));
  const guard = resolve(dir, 'no-network.mjs');
  writeFileSync(guard, 'globalThis.fetch=async()=>{process.exit(93)};'); // Distinguish any attempted transport from an expected refusal.
  for (const source of TARGETS) {
    const f = fixture(), m = plan(offlineSnapshot(f, source), f.decisions, key), file = resolve(dir, `${source.project}.enc.json`);
    writeFileSync(file, stable(seal(m, key)), { flag: 'wx' });
    for (const selected of TARGETS) {
      const child = spawnSync(process.execPath, ['--import', pathToFileURL(guard).href, 'tools/reservation-migration/cli.mjs', '--project', selected.project, '--database', selected.database, '--mode', 'inspect', '--manifest', file], {
        encoding: 'utf8', env: { ...process.env, AUTOCARE_MIGRATION_KEY: TEST_KEY_HEX, FIRESTORE_EMULATOR_HOST: selected.emulatorHost }
      });
      assert.equal(child.status, source === selected ? 0 : 1);
      if (source === selected) assert.equal(JSON.parse(child.stdout).manifestHash, m.hash);
    }
    const foreign = TARGETS.find(t => t !== source);
    const mixed = spawnSync(process.execPath, ['--import', pathToFileURL(guard).href, 'tools/reservation-migration/cli.mjs', '--project', source.project, '--database', source.database, '--mode', 'inspect', '--manifest', file], {
      encoding: 'utf8', env: { ...process.env, AUTOCARE_MIGRATION_KEY: TEST_KEY_HEX, FIRESTORE_EMULATOR_HOST: foreign.emulatorHost }
    });
    assert.equal(mixed.status, 1, 'Mixed CLI environment must fail before transport, never fall back');
  }
});
