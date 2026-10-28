// Deliberately no cloud adapter: every request uses one exact local demo tuple.
import { COLLECTIONS, POLICY } from './plan.mjs';
import { DEFAULT_TARGET, resolveTarget } from './targets.mjs';

export const HOST = DEFAULT_TARGET.host; // Original-target test exports are unchanged.
export const ROOT = DEFAULT_TARGET.root;
export class Emulator {
  constructor(options) {
    this.target = resolveTarget(options);
    this.host = this.target.host;
    this.root = this.target.root;
    Object.freeze(this);
  }
  async request(suffix, body, method = 'POST') {
    // suffix cannot select another host. Redirects and environment credentials are unused.
    const base = `/v1/${this.root}`;
    if (typeof suffix !== 'string' || !(suffix === base || suffix.startsWith(`${base}/`) || suffix.startsWith(`${base}:`)) || suffix.includes('..')) throw new Error('INVALID_LOCAL_ENDPOINT');
    const url = new URL(`${this.host}${suffix}`);
    if (suffix.includes('%') || url.pathname !== suffix || url.search || url.hash) throw new Error('INVALID_LOCAL_ENDPOINT');
    if ((body?.documents ?? []).some(name => !name.startsWith(`${this.root}/`))
      || (body?.writes ?? []).some(w => !w.update?.name?.startsWith(`${this.root}/`))) throw new Error('CROSS_TARGET_DOCUMENT_REFUSED');
    const response = await fetch(url.href, { method, redirect: 'error', headers: { authorization: 'Bearer owner', 'content-type': 'application/json' }, ...(body === undefined ? {} : { body: JSON.stringify(body) }), signal: AbortSignal.timeout(20_000) });
    const payload = await response.json();
    if (!response.ok) {
      // Never surface server messages: they can contain document paths or values.
      const error = new Error(`EMULATOR_REQUEST_FAILED_${payload.error?.status ?? response.status}`);
      error.code = payload.error?.status;
      throw error;
    }
    return payload;
  }
  async query(collection, transaction, parent = '') {
    const result = await this.request(`/v1/${this.root}${parent ? `/${parent}` : ''}:runQuery`, { structuredQuery: { from: [{ collectionId: collection }], limit: 202 }, ...(transaction ? { transaction } : {}) });
    const docs = result.filter(row => row.document).map(row => row.document);
    if (docs.length > 200) throw new Error('REHEARSAL_INVENTORY_LIMIT');
    if (docs.some(doc => !doc.name.startsWith(`${this.root}/`))) throw new Error('CROSS_TARGET_DOCUMENT_REFUSED');
    return Object.fromEntries(docs.map(doc => [doc.name.slice(this.root.length + 1), { fields: doc.fields ?? {}, updateTime: doc.updateTime }]));
  }
  async get(path, transaction) {
    const result = await this.request(`/v1/${this.root}:batchGet`, { documents: [`${this.root}/${path}`], ...(transaction ? { transaction } : {}) });
    const doc = result[0].found;
    if (doc && doc.name !== `${this.root}/${path}`) throw new Error('CROSS_TARGET_DOCUMENT_REFUSED');
    return doc ? { fields: doc.fields ?? {}, updateTime: doc.updateTime } : null;
  }
  async snapshot(transaction) {
    const docs = {};
    // Sequential reads remain in one transaction, with a bounded rehearsal inventory.
    for (const collection of COLLECTIONS) Object.assign(docs, await this.query(collection, transaction));
    const policy = await this.get(POLICY, transaction);
    if (Object.keys(docs).length > 200) throw new Error('REHEARSAL_INVENTORY_LIMIT');
    return { project: this.target.project, database: this.target.database, emulatorHost: this.target.emulatorHost, docs, policy };
  }
  async begin() { return (await this.request(`/v1/${this.root}:beginTransaction`, { options: { readWrite: {} } })).transaction; }
  async abort(transaction) { await this.request(`/v1/${this.root}:rollback`, { transaction }).catch(() => {}); }
  async readSnapshot() {
    // A read-only Firestore transaction creates no document/audit writes.
    const transaction = (await this.request(`/v1/${this.root}:beginTransaction`, { options: { readOnly: {} } })).transaction;
    try { return await this.snapshot(transaction); } finally { await this.abort(transaction); }
  }
  async journal(hash, transaction) { return this.query('entries', transaction, `reservationMigrationRuns/${hash}`); }
  async commit(writes, transaction) {
    if (!writes.length || writes.length > 10) throw new Error('INVALID_BATCH_SIZE');
    return this.request(`/v1/${this.root}:commit`, { writes, transaction });
  }
}
export function write(path, after, before, target = DEFAULT_TARGET) {
  if (!/^(bookings|slots|reservationMigrationRuns)\//.test(path)) throw new Error('WRITE_SCOPE_REFUSED');
  return { update: { name: `${resolveTarget(target).root}/${path}`, fields: after }, currentDocument: before ? { updateTime: before.updateTime } : { exists: false } };
}
