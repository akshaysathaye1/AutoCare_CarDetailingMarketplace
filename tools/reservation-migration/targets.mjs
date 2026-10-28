// The only supported environment tuples. Never derive a URL from user input.
function target(project, emulatorHost) {
  const database = '(default)';
  return Object.freeze({ project, database, emulatorHost, host: `http://${emulatorHost}`, root: `projects/${project}/databases/${database}/documents` });
}
export const DEFAULT_TARGET = target('demo-autocare-migration', '127.0.0.1:8788');
export const PERSON1_TARGET = target('demo-autocare-person1-migration', '127.0.0.1:8790');
export const TARGETS = Object.freeze([DEFAULT_TARGET, PERSON1_TARGET]);

export function resolveTarget({ project, database, emulatorHost, host, root } = {}) {
  const selected = TARGETS.find(t => t.project === project && t.database === database);
  if (!selected || (emulatorHost !== undefined && emulatorHost !== selected.emulatorHost)
    || (host !== undefined && host !== selected.host) || (root !== undefined && root !== selected.root)) throw new Error('ENVIRONMENT_REFUSED');
  return selected;
}
export function assertSameTarget(actual, expected) {
  const target = resolveTarget(actual);
  if (target !== resolveTarget(expected)) throw new Error('CROSS_TARGET_MANIFEST_REFUSED');
  return target;
}
