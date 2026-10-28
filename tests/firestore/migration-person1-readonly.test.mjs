// Real-emulator read-only integration. Requires the documented Person 1 setup and
// CLI dry-run artifacts. Never seeds, clears, commits, applies or restores data.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { Emulator } from '../../tools/reservation-migration/emulator.mjs';
import { DEFAULT_TARGET, PERSON1_TARGET } from '../../tools/reservation-migration/targets.mjs';
import { decode, keyFromHex, summary, unseal, validateManifest } from '../../tools/reservation-migration/plan.mjs';
import { dryRun, execute } from '../../tools/reservation-migration/execute.mjs';
import { fixture } from './migration-fixtures.mjs';

test('Person 1 real-emulator dry-run integration without database writes', async t => {
  assert.equal(process.env.FIRESTORE_EMULATOR_HOST, PERSON1_TARGET.emulatorHost);
  assert.ok(process.env.AUTOCARE_PERSON1_ARTIFACT_DIR, 'Existing dry-run artifacts are required; this suite never creates fixtures');
  const dir = resolve(process.env.AUTOCARE_PERSON1_ARTIFACT_DIR), key = keyFromHex(process.env.AUTOCARE_MIGRATION_KEY);
  const read = name => JSON.parse(readFileSync(resolve(dir, name), 'utf8'));
  const setup = unseal(read('setup-snapshot.enc.json'), key);
  const envelope = read('manifest.enc.json'), manifest = unseal(envelope, key);
  const decisions = read('synthetic-decisions.json');
  class ReadOnlyPerson1 extends Emulator {
    async request(suffix, body, method = 'POST') {
      assert.equal(this.target, PERSON1_TARGET);
      assert.equal(method, 'POST');
      assert.equal(body?.writes, undefined, 'No document writes are permitted');
      const operation = suffix.slice(suffix.lastIndexOf(':') + 1);
      assert.ok(['beginTransaction', 'rollback', 'runQuery', 'batchGet', 'listCollectionIds'].includes(operation));
      if (operation === 'beginTransaction') {
        assert.deepEqual(body.options, { readOnly: {} }, 'Only read-only transaction cleanup is permitted, never a migration rollback');
      }
      return super.request(suffix, body, method);
    }
  }
  const db = new ReadOnlyPerson1(PERSON1_TARGET);
  const collections = async () => {
    const result = await db.request(`/v1/${db.root}:listCollectionIds`, { pageSize: 100 });
    assert.ok(!result.nextPageToken, 'Unexpected extra collections require investigation');
    return (result.collectionIds ?? []).sort();
  };
  const initial = await db.readSnapshot(), roots = await collections();
  // A baseline captured immediately after setup proves the CLI did not rewrite data.
  assert.deepEqual(initial, setup);
  t.after(async () => {
    assert.deepEqual(await db.readSnapshot(), setup, 'Database fields/versions must remain exactly as seeded');
    assert.deepEqual(await collections(), roots);
  });
  await t.test('actual endpoint, project, database and snapshot target are Person 1', () => {
    assert.equal(db.host, 'http://127.0.0.1:8790');
    assert.equal(initial.project, 'demo-autocare-person1-migration');
    assert.equal(initial.database, '(default)');
    assert.equal(initial.emulatorHost, '127.0.0.1:8790');
  });
  await t.test('all persisted documents match the documented synthetic setup', () => {
    const f = fixture();
    assert.deepEqual(Object.keys(initial.docs).sort(), Object.keys(f.docs).sort());
    for (const [path, data] of Object.entries(f.docs)) assert.deepEqual(decode(initial.docs[path].fields), data);
    assert.deepEqual(decode(initial.policy.fields), f.policy);
    assert.deepEqual(roots, ['bookings', 'garages', 'reservationPolicies', 'services', 'slots', 'users', 'vehicles'].sort());
  });
  await t.test('fresh encrypted manifest is authenticated, format 2 and bound to Person 1', () => {
    validateManifest(manifest, key, PERSON1_TARGET);
    assert.equal(manifest.format, 2);
    assert.equal(manifest.emulatorHost, PERSON1_TARGET.emulatorHost);
    assert.equal(manifest.project, PERSON1_TARGET.project);
    assert.equal(manifest.database, PERSON1_TARGET.database);
    assert.throws(() => validateManifest(manifest, key, DEFAULT_TARGET), /CROSS_TARGET/);
    assert.ok(!JSON.stringify(envelope).includes('DO_NOT_LOG_SYNTHETIC_PERSON'));
  });
  await t.test('one pair proposes two metadata writes, with no blockers or warnings', () => {
    const report = summary(manifest);
    assert.equal(report.units, 1); assert.equal(report.writes, 2); assert.equal(report.blockers, 0);
    assert.deepEqual(report.issues, []);
    const entry = manifest.entries[0]; assert.equal(entry.guards.length, 2);
    for (const op of entry.operations) {
      assert.match(op.path, /^(bookings|slots)\//);
      const data = decode(op.after);
      assert.equal(data.reservationVersion, 2); assert.equal(data.timeZoneId, 'Asia/Kolkata');
    }
  });
  await t.test('real read-only planning is deterministic against the same seeded versions', async () => {
    assert.deepEqual(await dryRun(db, decisions, key), manifest);
    assert.deepEqual(await dryRun(db, decisions, key), manifest);
  });
  await t.test('default executor returns only the dry-run summary', async () => {
    assert.deepEqual(await execute(db, manifest, key), summary(manifest));
  });
  await t.test('rollout remains disabled and no migration audit receipt was created', async () => {
    assert.equal(decode((await db.get('reservationPolicies/current')).fields).legacyMigrationComplete, false);
    assert.deepEqual(await db.journal(manifest.hash), {});
  });
  await t.test('fixture values and document versions are unchanged since setup', async () => {
    assert.deepEqual(await db.readSnapshot(), setup);
    assert.deepEqual(await collections(), roots);
  });
});
