import { decode, equal, fields, plan, seal, stable, summary, unseal, validateManifest } from './plan.mjs';
import { write } from './emulator.mjs';
import { assertSameTarget, resolveTarget } from './targets.mjs';

const journalPath = (m, entry) => `reservationMigrationRuns/${m.hash}/entries/${entry.id}`;
const retired = after => ({ ...after, ...fields({ isReserved: false, bookingId: '', status: 'retired' }) });
function assertDisabled(snapshot) {
  if (decode(snapshot.policy?.fields).legacyMigrationComplete === true) throw new Error('ROLLOUT_MUST_BE_DISABLED');
}
function records(manifest, journal, key) {
  const result = new Map();
  for (const [path, doc] of Object.entries(journal)) {
    const data = decode(doc.fields), entry = manifest.entries.find(e => journalPath(manifest, e) === path);
    if (!entry || data.manifestHash !== manifest.hash || !['applied', 'rolledBack'].includes(data.state)) throw new Error('UNRECOGNIZED_JOURNAL');
    const payload = unseal(JSON.parse(data.payload), key);
    if (!equal(payload.entry, entry) || payload.manifestHash !== manifest.hash) throw new Error('JOURNAL_INTEGRITY_FAILURE');
    if (data.state === 'rolledBack') {
      const recovery = unseal(JSON.parse(data.recoveryPayload), key);
      if (!equal(recovery.entry, entry) || recovery.manifestHash !== manifest.hash || typeof recovery.appliedVersion !== 'string') throw new Error('RECOVERY_JOURNAL_INTEGRITY_FAILURE');
    }
    result.set(entry.id, { ...doc, state: data.state });
  }
  return result;
}
function expectedInventory(manifest, journalRecords) {
  const expected = structuredClone(manifest.baseline);
  for (const entry of manifest.entries) {
    const record = journalRecords.get(entry.id);
    if (!record) continue;
    if (record.state !== 'applied') throw new Error('ROLLED_BACK_MANIFEST_CANNOT_BE_REAPPLIED');
    for (const op of entry.operations) expected[op.path] = { fields: op.after, updateTime: record.updateTime };
  }
  return expected;
}
function checkOriginal(manifest, snapshot, journalRecords) {
  // Detect new/deleted records and dependency changes, not just changed write targets.
  if (!equal(snapshot.docs, expectedInventory(manifest, journalRecords)) || !equal(snapshot.policy, manifest.policy)) throw new Error('INVENTORY_OR_VERSION_CHANGED_REPLAN_REQUIRED');
}
function checkRollback(entry, record, snapshot, manifest) {
  for (const path of entry.dependencies) {
    if (!equal(snapshot.docs[path], manifest.baseline[path])) throw new Error('POST_MIGRATION_DEPENDENCY_REQUIRES_RECONCILIATION');
  }
  for (const guard of entry.guards) {
    const current = snapshot.docs[guard.path];
    const op = entry.operations.find(op => op.path === guard.path);
    // A changed member shares the apply/rollback receipt's commit version.
    // An unchanged member keeps its captured baseline version (never the receipt's).
    // Full raw-field equality also protects ownership and reservation linkage.
    const expected = guard.expectedVersion.source === 'baseline'
      ? { fields: manifest.baseline[guard.path].fields, updateTime: guard.expectedVersion.updateTime }
      : { fields: record.state === 'rolledBack' ? op.before?.fields ?? retired(op.after) : op.after, updateTime: record.updateTime };
    if (!current || !equal(current, expected)) throw new Error('POST_MIGRATION_ACTIVITY_REQUIRES_RECONCILIATION');
  }
}
function checkRollbackRun(manifest, journalRecords, snapshot) {
  // Preflight and every rollback transaction verify ALL journaled pairs, including
  // completed rollback units. No skip/resume path weakens unchanged-member guards.
  for (const entry of manifest.entries) {
    const record = journalRecords.get(entry.id);
    if (record) checkRollback(entry, record, snapshot, manifest);
  }
}
function journalFields(manifest, entry, state, key, options) {
  return fields({ manifestHash: manifest.hash, state, payload: stable(seal({ manifestHash: manifest.hash, entry, operator: options.operator, maintenanceEvidence: options.maintenanceEvidence }, key)) });
}
function recoveryFields(manifest, entry, record, key, options) {
  // Preserve the original encrypted apply receipt, including its operator/evidence.
  return { ...record.fields, ...fields({ state: 'rolledBack', recoveryPayload: stable(seal({ manifestHash: manifest.hash, entry, appliedVersion: record.updateTime, operator: options.operator, maintenanceEvidence: options.maintenanceEvidence }, key)) }) };
}
export async function dryRun(emulator, decisions, key) {
  const target = resolveTarget(emulator.target);
  const snapshot = await emulator.readSnapshot();
  assertSameTarget(snapshot, target);
  return plan(snapshot, decisions, key);
}
export async function execute(emulator, manifest, key, options = {}) {
  const target = resolveTarget(emulator.target);
  validateManifest(manifest, key, target);
  // API callers must opt in too. The default never creates an audit record.
  if (options.mode === undefined || options.mode === 'dry-run') return summary(manifest);
  if (!['apply', 'rollback'].includes(options.mode) || options.approveManifest !== manifest.hash || typeof options.maintenanceEvidence !== 'string' || !options.maintenanceEvidence.trim() || typeof options.operator !== 'string' || !options.operator.trim()) throw new Error('EXPLICIT_REVIEW_AND_MAINTENANCE_ACK_REQUIRED');
  if (manifest.issues.some(i => i.blocking)) throw new Error('UNRESOLVED_BLOCKING_ISSUES');
  if (options.maxUnits !== undefined && (!Number.isInteger(options.maxUnits) || options.maxUnits < 1)) throw new Error('INVALID_MAX_UNITS');
  const preflight = await emulator.begin();
  let initial;
  try {
    const snapshot = await emulator.snapshot(preflight);
    assertSameTarget(snapshot, emulator.target);
    assertDisabled(snapshot);
    initial = records(manifest, await emulator.journal(manifest.hash, preflight), key);
    if (options.mode === 'apply') checkOriginal(manifest, snapshot, initial);
    else checkRollbackRun(manifest, initial, snapshot);
  } finally { await emulator.abort(preflight); }
  let completed = 0, skipped = 0;
  const ordered = options.mode === 'apply' ? manifest.entries : [...manifest.entries].reverse();
  for (const entry of ordered) {
    if (completed >= (options.maxUnits ?? Infinity)) break;
    const transaction = await emulator.begin();
    try {
      // All inventory/journal/policy reads precede every write.
      const snapshot = await emulator.snapshot(transaction);
      assertSameTarget(snapshot, emulator.target);
      const journal = records(manifest, await emulator.journal(manifest.hash, transaction), key);
      assertDisabled(snapshot);
      const record = journal.get(entry.id);
      if (options.mode === 'apply') {
        checkOriginal(manifest, snapshot, journal);
        if (record) { skipped++; await emulator.abort(transaction); continue; }
        const writes = entry.operations.map(op => write(op.path, op.after, op.before, emulator.target));
        writes.push(write(journalPath(manifest, entry), journalFields(manifest, entry, 'applied', key, options), null, emulator.target));
        await emulator.commit(writes, transaction);
      } else {
        checkRollbackRun(manifest, journal, snapshot);
        if (!record || record.state === 'rolledBack') { skipped++; await emulator.abort(transaction); continue; }
        // Never delete. Restore modified originals; quarantine newly created canonical slots.
        const writes = entry.operations.map(op => write(op.path, op.before?.fields ?? retired(op.after), snapshot.docs[op.path], emulator.target));
        writes.push(write(journalPath(manifest, entry), recoveryFields(manifest, entry, record, key, options), record, emulator.target));
        await emulator.commit(writes, transaction);
      }
      completed++;
    } catch (error) { await emulator.abort(transaction); throw error; }
  }
  // No completion-policy write exists anywhere in this tool.
  return { manifestHash: manifest.hash, mode: options.mode, completed, skipped, reconciliationRequired: options.mode === 'rollback' };
}
