import { readFileSync, writeFileSync } from 'node:fs';
import { parseArgs } from 'node:util';
import { Emulator } from './emulator.mjs';
import { keyFromHex, seal, stable, summary, unseal, validateManifest } from './plan.mjs';
import { dryRun, execute } from './execute.mjs';

try {
  const { values } = parseArgs({ options: {
    project: { type: 'string' }, database: { type: 'string' }, mode: { type: 'string', default: 'dry-run' },
    decisions: { type: 'string' }, output: { type: 'string' }, manifest: { type: 'string' },
    'approve-manifest': { type: 'string' }, 'maintenance-evidence': { type: 'string' }, 'max-units': { type: 'string' }, operator: { type: 'string' }
  } });
  const emulator = new Emulator({ project: values.project, database: values.database, emulatorHost: process.env.FIRESTORE_EMULATOR_HOST });
  const key = keyFromHex(process.env.AUTOCARE_MIGRATION_KEY);
  if (!['dry-run', 'inspect', 'apply', 'rollback'].includes(values.mode)) throw new Error('UNKNOWN_MODE');
  if (values.mode === 'dry-run') {
    if (!values.output || values.manifest) throw new Error('NEW_ENCRYPTED_OUTPUT_REQUIRED');
    const decisions = values.decisions ? JSON.parse(readFileSync(values.decisions, 'utf8')) : {};
    const manifest = await dryRun(emulator, decisions, key);
    writeFileSync(values.output, stable(seal(manifest, key)), { flag: 'wx', mode: 0o600 });
    console.log(stable(summary(manifest)));
    if (manifest.issues.some(i => i.blocking)) process.exitCode = 2;
  } else {
    if (!values.manifest || values.output || values.decisions) throw new Error('EXISTING_MANIFEST_REQUIRED');
    const manifest = unseal(JSON.parse(readFileSync(values.manifest, 'utf8')), key);
    validateManifest(manifest, key, emulator.target);
    console.log(stable(values.mode === 'inspect' ? summary(manifest) : await execute(emulator, manifest, key, {
      mode: values.mode, approveManifest: values['approve-manifest'], maintenanceEvidence: values['maintenance-evidence'], operator: values.operator,
      ...(values['max-units'] === undefined ? {} : { maxUnits: Number(values['max-units']) })
    })));
  }
} catch {
  // Detailed diagnostics can contain decrypted customer values/paths. Do not print them.
  console.error('Reservation rehearsal refused or failed. Check environment, approval, blockers and version preconditions; inspect the protected manifest in an authorized process.');
  process.exitCode = 1;
}
