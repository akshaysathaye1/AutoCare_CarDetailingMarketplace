// Explicit synthetic seed command for an EMPTY, fixed local demo emulator.
import { mkdirSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { parseArgs } from 'node:util';
import { Emulator } from '../../tools/reservation-migration/emulator.mjs';
import { stable } from '../../tools/reservation-migration/plan.mjs';
import { fixture, seed } from './migration-fixtures.mjs';

try {
  const { values } = parseArgs({ options: { project: { type: 'string' }, database: { type: 'string' }, 'output-dir': { type: 'string' } } });
  const db = new Emulator({ project: values.project, database: values.database, emulatorHost: process.env.FIRESTORE_EMULATOR_HOST });
  if (!values['output-dir']) throw new Error('OUTPUT_REQUIRED');
  const snapshot = await db.readSnapshot();
  if (Object.keys(snapshot.docs).length || snapshot.policy) throw new Error('EMPTY_SYNTHETIC_EMULATOR_REQUIRED');
  const dir = resolve(values['output-dir']), f = fixture();
  mkdirSync(dir, { recursive: true });
  writeFileSync(resolve(dir, 'synthetic-decisions.json'), stable(f.decisions), { flag: 'wx', mode: 0o600 });
  await seed(db, f);
  console.log('Seeded one synthetic legacy booking and reservation; rollout remains disabled.');
} catch {
  console.error('Synthetic seed refused: require an empty local demo emulator and a new output directory.');
  process.exitCode = 1;
}
