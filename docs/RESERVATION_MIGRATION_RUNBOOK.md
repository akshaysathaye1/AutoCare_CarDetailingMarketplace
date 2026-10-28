# Reservation migration preparation and isolated rehearsal

Status: preparation only. This runbook and utility do not authorize production access,
deployment, migration, API enablement, IAM changes or opening the rollout gate.
No production credentials or customer data are needed for the supplied commands.

## Supported implementation and boundaries

The dependency-free Node utility in `tools/reservation-migration/` is an
**emulator-only reference implementation**, following the repository's REST test
architecture. `targets.mjs` permits exactly two immutable local tuples:

| Purpose | Project | Endpoint | Database |
| --- | --- | --- | --- |
| Existing target, unchanged | `demo-autocare-migration` | `http://127.0.0.1:8788` | `(default)` |
| Person 1 disposable instance | `demo-autocare-person1-migration` | `http://127.0.0.1:8790` | `(default)` |

Both are synthetic-only, at most 200 inventory documents and 200,000 bytes of
snapshot JSON. Project and database arguments remain mandatory. The project selects
its fixed allowlisted endpoint; an optional `FIRESTORE_EMULATOR_HOST` must match that
same tuple exactly. A stale `127.0.0.1:8788` environment value with the Person 1 project
is rejected, rather than falling back to the existing instance. Arbitrary hosts,
projects/databases, mixed tuples and redirects are refused. The tool
never reads ADC, `google-services.json`, Firebase project defaults, service-account
files or production credentials. There is no cloud adapter and no deployment command.
Do not remove these restrictions to turn this preparation into a production tool.
A separately reviewed cloud/staging adapter and operational approvals remain required.

The emulator's `Bearer owner` fixture access bypasses rules **only on the selected
allowlisted local endpoint**. It does not demonstrate production IAM authorization. Current
rules deny application access to the migration journal's unmatched collection.

## Confirmed schema and classification

The source contract remains `docs/RESERVATION_SECURITY.md`, `firestore.rules`,
`Booking.kt`, `SlotReservation.kt`, `BookingValidator.kt` and `BookingRepository.kt`.
Version 2 uses `Asia/Kolkata`, canonical `yyyy-MM-dd`, `hh:mm AM/PM`, and epoch
milliseconds of India midnight. The slot ID retains its existing length-prefixed
SHA-256/slug encoding, including the first 16 lowercase digest characters.
Example regression vector:

```text
garage_apex / 2026-10-30 / 11:00 AM
slot_garage_apex_2026-10-30_1100AM_9c2f95b7ef65bbc5
India midnight = 2026-10-29T18:30:00.000Z
```

No appointment is inferred from a legacy epoch, an ID suffix, or an unverified
legacy `dateString`. Canonical metadata must be internally consistent; a record
claiming version 2 with an invalid date/timezone is blocked for separate review.

Inspection also covered `SlotReservationStore.kt`, the actual-mapper/legacy-vector
tests in `ReservationSecurityTest.kt`, the existing `tests/firestore/reservations.test.mjs`,
and write paths in `GarageRepository.kt`, `ServiceRepository.kt`, `VehicleRepository.kt`,
`AuthRepository.kt`, `ReviewRepository.kt` and `MockDataInjector.kt`. Repository searches
found no checked-in cloud migration/server writer implementation to freeze on the
administrator's behalf; external console access, jobs and identities remain unknown.
Kotlin source paths are under
`app/src/main/java/com/example/mad_project_akshaysathaye_c049/`, with tests in the
matching `app/src/test/java` package. Existing application/rule/test files were not
modified by this preparation; only the security-document cross-reference was added.

| Record class | Tool treatment |
| --- | --- |
| Consistent canonical active pair | No changes; still claims its canonical target for conflict detection |
| Legacy pending/confirmed/in_progress booking | Requires approved local date/time and verified customer, vehicle, workshop, service and owner-role references |
| Overdue active booking | Still active; never automatically expired, cancelled or freed |
| Legacy available slot | Inspected independently; requires date/time evidence and a real workshop before normalization |
| `reserved` Boolean alias | Accepted only with a consistent state; canonical output uses `isReserved`; contradictory aliases block |
| Multiple linked slots, occupied canonical target or competing target claims | Blocks the entire manifest; no customer is chosen |
| Missing reservation | Blocks unless a reviewer explicitly approves `createMissingSlot` with evidence |
| Orphan reserved slot / cancelled booking still reserved | Blocks; no automatic deletion or release |
| Completed booking with retained reservation | Preserved as historical, reported for sign-off; current completion logic does not release it |
| Cancelled/completed booking without reservation changes | Preserved; never assigned a guessed slot |
| Unknown status, missing/foreign ownership, invalid public slot fields | Blocks for administrator investigation |
| Changed legacy slot ID | Requires explicit `retireLegacySlot` approval; retains source document and creates canonical target atomically |

Booking IDs, existing correct slot IDs, prices, customer ownership, notes and
historical snapshots are preserved. Public canonical slots contain only the strict
slot schema, never booking/customer payloads. Existing slot timestamps are preserved
where valid; a newly reconstructed slot uses `updatedAt: 0` rather than an invented
historical time. The private journal's server update version records migration time.

Completed/retired history warnings are **not a claim of operational suitability**.
An administrator must explicitly review future-dated completed records, stale
reservations, retention and capacity implications before any later rollout approval.
Duration overlaps, bays, holidays and operating hours remain outside this identity model.

## Decisions and deterministic manifests

An authorized reviewer supplies decisions in a protected JSON file. Paths and
evidence can identify records, so keep it out of Git and ordinary logs. Only the
synthetic fixture below may be copied freely:

```json
{
  "bookings/booking_fixture_1": {
    "dateString": "2031-08-06",
    "scheduledTime": "11:00 AM",
    "approvedBy": "synthetic-reviewer",
    "evidence": "synthetic-appointment-1"
  }
}
```

`createMissingSlot: true` and `retireLegacySlot: true` are separate, explicit
decisions, never defaults. Ownership mismatches, contradictory booking/slot raw
dates or links, multiple slots and existing canonical-target conflicts cannot be
overridden with these flags. A separate administrator reconciliation is required.
Do not use a generic approval string in place of actual evidence.

The decrypted manifest is deterministic for a fixed snapshot, key and decisions:

- environment and format version;
- complete scoped baseline and policy, including Firestore `updateTime` versions;
- approved decisions and classified issues;
- ordered migration units with HMAC-protected references, reason codes, dependencies,
  original raw Firestore fields, proposed fields and existence/version preconditions;
- complete booking/slot pair guards, including partners that require no write;
- keyed manifest digest for review approval.

The authenticated environment includes `project`, `database` and `emulatorHost`.
Planning, adapter snapshots, fixture serialization and audit/write paths all use the
selected tuple. Validated CLI inspection and execution require the selected adapter
to match the manifest before any execution reads, even for dry-run/default execution
or resumption. Requests and returned document names must belong to that adapter's
project/database namespace. A manifest cannot be carried from one approved target
to the other. Older format-2 manifests lacking `emulatorHost` are also rejected;
regenerate unexecuted plans and reapprove their new digest. Previously applied older
plans require manual audit, not a silent metadata upgrade.

Manifest format **2** is required after the rollback-guard correction. This is the
tool's manifest format, separate from the unchanged reservation version 2 and
encryption envelope format 1. Pre-fix manifests are rejected for apply, resume,
rollback and validated CLI inspection; do not edit or re-sign them to bypass the
check. Regenerate an unexecuted plan from a fresh synthetic snapshot and obtain a
new digest approval. An old partially applied run requires protected manual audit
and reconciliation; do not silently adopt its records into a new run.

For each unit, `operations` contains only actual writes, while `guards` retains
the booking, canonical slot and any legacy slot to be retired **before** unchanged
operations are filtered. Each guard records an expected-version source:

- `baseline` includes the captured `updateTime` of an unchanged partner. Its expected
  raw fields remain in the authenticated manifest baseline. It is never assigned
  the migration receipt's newer version and never receives a forced no-op write.
- `applyCommit` identifies a written/new document. Its expected fields are in the
  write operation; its actual expected version is the server `updateTime` of the
  audit receipt committed atomically with those writes.

These guards are included in the keyed digest and encrypted audit payload. A unit
with no writes at all needs no recovery receipt because it will never be restored.

The persisted manifest is AES-256-GCM encrypted and authenticated. A fresh IV makes
ciphertext differ even when the decrypted plan/digest is identical. HMAC references
and digests resist guessing from predictable customer IDs without the private key.
Encryption and reference derivation use separate domains. Preserve the key privately
for the entire recovery period; loss of the key prevents recovery from the journal.

The CLI prints only aggregate counts, issue codes and protected references. `inspect`
also prints a redacted summary. An authorized reviewer can use the module's `unseal`
function in an access-controlled local process to inspect raw before/after values;
do not dump decrypted output into terminals, tickets, chat or CI artifacts. Review
synthetic examples directly in the fixture source. `mode: 0600` is requested for
new artifacts; administrators must separately verify Windows ACLs and backup ACLs.

## Maintenance freeze: required operational design

The rollout policy blocks new booking creation and slot reservation/re-reservation
only after the hardened rules are actually deployed. It does not freeze all writes.
This preparation changes neither the rules nor their deployed state.

| Writer or operation | What must be controlled during a future maintenance window |
| --- | --- |
| `createBooking` and direct/offline client writes | Keep policy absent/false under deployed hardened rules; verify server denial, including old clients and queued writes |
| Customer cancellation / owner decline | These atomically change bookings and slots without consulting the gate; pause or use an audited support queue |
| Owner confirm/start/complete and notes | These can update bookings while the gate is off; server-side maintenance controls are required |
| Service create/edit/delete, price, duration, availability aliases | Pause all writers to prevent dependency changes |
| Vehicle create/edit/delete | Pause writes; do not repair ownership by reassigning a vehicle |
| Garage create/edit, hours/open state and privileged ownership changes | Pause client and administrative writers |
| Registration, user profile/role changes and Auth deletion/disablement | Pause relevant flows and administrative jobs; Auth is outside the tool's Firestore inventory and requires separate verification |
| Mock/demo injectors, imports, scripts, background/server jobs, admin consoles | Stop or revoke execution through an approved operational control; privileged writers bypass rules |
| Reviews and aggregate handlers | Reviews read booking state; pause any handler that changes scoped dependencies. Booking IDs are retained so review links do not need renaming |
| Rollout policy and migration execution | Only the named maintenance operator; no other administrator changes during the run |

An app maintenance banner alone is insufficient. Before a cloud rehearsal, an
administrator must approve and implement a temporary, tested server-side deny policy
for ordinary writes across the six scoped collections, plus control all IAM-based
writers. No such temporary rule set or IAM change is implemented here. Verify denial
with approved customer/owner clients and paused service identities, then record the
freeze evidence and a stable inventory. After migration, restore the reviewed hardened
permissions while keeping reservation creation disabled until final sign-off.

Every apply unit reads the entire bounded six-collection inventory, policy and
journal in a real transaction. It compares them with the baseline plus already
applied units. New/deleted records and dependency edits force a stop and replan.
This is a secondary guard, **not proof that all external writers are frozen**.
Subcollections, Auth, external systems and large production datasets are not inventoried.

## Backup and isolated restoration acceptance

Before a future cloud execution, require named operator/reviewer/recovery owner,
approved environment, IAM review, maintenance duration and stop thresholds.

1. Establish a consistent cutoff after relevant writers are frozen, or use an
   independently verified existing point-in-time recovery method. A normal managed
   export can include writes made while it runs; do not assume it is a snapshot.
2. Back up bookings, slots, policy and required user/workshop/service/vehicle references.
   Define coverage of subcollections, Auth, reviews and external dependencies explicitly.
3. Save reviewed rules, indexes, migration code hashes and configuration separately.
   Firestore document exports do not include index definitions.
4. Record completed operation IDs, cutoff, counts, artifact checksums, storage location,
   retention, encryption/key custody and access controls. Source-code copies are not DB backups.
5. Restore to a separately authorized isolated project, confirm identity/reference counts,
   compare protected per-record checksums and verify no production client or integration
   can connect. Prevent outbound notifications or other side effects.
6. Rehearse migration, interruption and inverse recovery there. Record observed results,
   elapsed time, failure conditions and the recovery decision process.

No backup export, restore, production inventory or cloud rehearsal was performed by
this preparation. Do not enable unavailable APIs or recovery features without separate approval.

Official behavior references:
- [Firestore export/import](https://firebase.google.com/docs/firestore/manage-data/export-import)
- [Transaction queries](https://firebase.google.com/docs/firestore/reference/rest/v1/projects.databases.documents/runQuery)
- [Version preconditions](https://firebase.google.com/docs/firestore/reference/rest/v1/Precondition)
- [Write result versions, including unchanged writes](https://firebase.google.com/docs/firestore/reference/rest/v1/WriteResult)
- [Rules and privileged server access](https://firebase.google.com/docs/firestore/security/rules-conditions)

## Controlled application, resumption and audit

`dry-run` is the CLI and API default and writes no Firestore documents, including no
journal. It saves an encrypted local manifest exclusively, refusing existing output
files. Any blocker exits with status 2. Refusals/other failures exit with status 1.
Unresolved blockers prevent **all** apply units, even otherwise valid ones.

Apply requires the exact reviewed manifest digest, an operator identifier and a
maintenance-evidence reference. These assertions are journaled privately; they are
not authentication or independent proof of a freeze. The emulator has no IAM audit.

Each unit performs at most three business-document writes plus one private journal
write. Existing documents use `currentDocument.updateTime`; new canonical targets
and journal entries use `exists: false`. Business writes and encrypted audit are
atomic. The audit is under:

```text
reservationMigrationRuns/{manifestDigest}/entries/{protectedEntryReference}
```

The payload contains originals, proposals, reason/evidence references and operator
acknowledgment, including guards for unchanged partners. Only keyed references,
digest and state are plaintext. Application
clients have no access under current rules. Cloud IAM/retention must be separately reviewed.

`--max-units 1` bounds a rehearsal batch. Rerun the **same encrypted manifest and
same key** after interruption. Applied units are skipped only after checking the
journal and exact document versions/content. A lost commit acknowledgment is resolved
from the atomic journal. No automatic retry reinterprets a failed precondition.
If any dependency/inventory changed, stop and review the partial-run journal before
creating a new plan; do not conceal changes by regenerating approvals blindly.

No code path writes `reservationPolicies/current`, deploys rules or calls a cloud API.

## Retirement and rollback

Different legacy IDs are never deleted. With explicit approval, the old slot remains
at its original path, with original metadata retained and its live fields changed to
`isReserved: false`, `bookingId: ""`, `status: "retired"`. Original values are encrypted
in the journal. The canonical slot and booking link are written in the same unit.
`retired` intentionally fails the app's v2 availability check and the rules' slot
schema; it cannot be booked or released by an application client. It is a quarantine
state for administrative review, not a new application lifecycle feature. Legacy
`reserved` aliases may remain on retired originals as historical fields; consumers
must honor the maintenance and compatibility restrictions.

This preparation does not move source records to a new archive collection or delete
them. A later archive policy needs authorized retention, access and compatibility
decisions. Records with unexpected public fields are blocked rather than copied.

Rollback requires a separately explicit mode and approval while the gate is disabled:

1. Freeze relevant activity again and preserve a fresh protected post-cutover inventory.
2. Review the journal and identify new activity since application.
3. Preflight every journaled unit. Both booking and slot partners must match their
   expected versions and complete raw fields, including ownership/linkage. Written
   partners use the receipt's commit version; unchanged partners use their captured
   baseline version. Dependencies must match the baseline. Check every journaled
   pair again inside each rollback transaction before queuing any recovery write.
4. Restore modified original records with current version preconditions.
5. Newly created canonical slots are **retained and quarantined as retired**, never
   silently removed. They are no longer active reservations but require reconciliation
   before any subsequent reopening.
6. Update the journal atomically to `rolledBack`, retaining the original encrypted
   apply receipt and adding an encrypted recovery receipt. Interrupted rollback can
   resume; finished units are rechecked before being skipped. For finished units,
   restored/quarantined write targets must match the rollback receipt's version and
   expected restored/quarantined fields; unchanged partners keep their baseline
   versions. A rolled-back manifest cannot be applied again.
7. Preserve unrelated records created after the run. A changed/reused migrated slot,
   modified booking, ownership dependency change, or change-and-restore (ABA) stops
   recovery for explicit reconciliation. Do not overwrite later customers' activity.

Rollback units run in reverse order; preflight catches known conflicts before writing,
and each transaction rechecks all journaled pairs, including unchanged partners in
already-rolled-back units. A conflict stops the transaction without restoring or
quarantining either member of the affected pair or changing its receipt. Earlier
committed recovery units are not undone automatically: a failure during recovery
can leave an audited partial recovery, not an all-database atomic rollback.
Post-rollback/new activity must still be included in the final review. Changes to
records outside the guarded pairs and dependencies are preserved and do not by
themselves block rollback; apply/resume still requires the complete scoped inventory
to match. New external references, Auth changes and other uninventoried activity
require manual reconciliation and an effective maintenance freeze.
The tool relies on matching update versions of atomic changed documents and journal;
this was verified in the local emulator. If versions cannot establish provenance,
recovery fails closed. This is not a claim about an unimplemented cloud adapter.

The corrected regression is a canonical booking plus a slot with a legacy Boolean
alias. Migration writes only the slot. A later authorized owner confirmation changes
the untouched booking while the rollout gate remains disabled. Rollback now rejects
the changed booking version before restoring the slot, and leaves both records and
the journal unchanged. The same protection applies to an unchanged slot in a
booking-only migration and detects change-and-restore (ABA) activity.

A backup import is **not sufficient rollback**: it can overwrite matching IDs while
leaving new canonical documents in place. Retain the manifest, inverse-write journal,
post-cutover reconciliation and backups. Never restore permissive legacy rules.

## Evidence before enabling the production completion flag

An authorized administrator, outside this tool, must approve:

- exact deployed rule/app/tool versions, project and database;
- verified write freeze, consistent backup and successful isolated restoration;
- reviewed manifest with no blockers, complete applied journal and no partial units;
- full protected inventory verification, not a sample: every in-scope active booking
  has exactly one canonical reserved slot, links/identity agree, and no other active
  slot points to that booking;
- canonical available slots have empty booking links; cancelled records do not retain
  active reservations; completed/retired exemptions and legacy history are approved;
- zero unresolved conflicts, orphans, aliases, identity/ownership anomalies or unknown
  states; before/after totals account for newly created and retired records;
- tested authorization, concurrency, failure, rollback and device-timezone behavior;
- compatible clients, approved support path, monitoring thresholds and recovery owner;
- explicit resolution of all retained/quarantined slots, post-cutover activity and
  scheduling limitations affecting reopening.

The completion flag is an administrator attestation, not an automatic validation
result. This tool never sets it and a successful batch is insufficient evidence.

## Exact local commands (PowerShell, repository root)

Prerequisites: Node with built-in fetch/test runner, the already downloaded official
emulator JAR described in `RESERVATION_SECURITY.md`, and Android Studio's Java runtime.
No npm packages are required. The migration emulator is separate from the existing
rules suite's project/port. All data here is synthetic.

Terminal 1:

```powershell
Set-Location 'C:\Users\asus\Desktop\MAD Project\AutoCare_CarDetailingMarketplace'
& 'C:\Program Files\Android\Android Studio\jbr\bin\java.exe' -jar '.gradle/security-fix-before/firestore-emulator.jar' --host 127.0.0.1 --port 8788 --project_id demo-autocare-migration --single_project_mode --single_project_mode_error --rules firestore.rules
```

Terminal 2, run tests **only against a disposable instance**: the emulator suite
clears that fixed local demo database between scenarios, never a cloud database.

```powershell
Set-Location 'C:\Users\asus\Desktop\MAD Project\AutoCare_CarDetailingMarketplace'
node --test tests/firestore/migration-plan.test.mjs
node --test tests/firestore/migration-emulator.test.mjs
```

For a manual example, stop and restart Terminal 1 without any import to get an
empty emulator; do not run this against the tests' remaining fixtures. In Terminal 2:

```powershell
$env:FIRESTORE_EMULATOR_HOST = '127.0.0.1:8788'
$env:AUTOCARE_MIGRATION_KEY = & node -e 'process.stdout.write(require("node:crypto").randomBytes(32).toString("hex"))'
node tests/firestore/migration-example.mjs --project demo-autocare-migration --database '(default)' --output-dir '.gradle/reservation-migration-demo'
node tools/reservation-migration/cli.mjs --project demo-autocare-migration --database '(default)' --decisions '.gradle/reservation-migration-demo/synthetic-decisions.json' --output '.gradle/reservation-migration-demo/manifest.enc.json'
node tools/reservation-migration/cli.mjs --project demo-autocare-migration --database '(default)' --mode inspect --manifest '.gradle/reservation-migration-demo/manifest.enc.json'
```

The observed shape is `units: 1`, `writes: 2`, `blockers: 0`; the digest depends on
the key and emulator document versions. The encrypted manifest contains original
fields plus the proposed v2 metadata; neither command applies the migration.
The seed command itself explicitly writes synthetic fixtures to an empty emulator.
The output path must be new; use another private directory for a later example.

Optional **synthetic emulator** application, interruption/resumption and rollback:

```powershell
$report = node tools/reservation-migration/cli.mjs --project demo-autocare-migration --database '(default)' --mode inspect --manifest '.gradle/reservation-migration-demo/manifest.enc.json' | ConvertFrom-Json
node tools/reservation-migration/cli.mjs --project demo-autocare-migration --database '(default)' --mode apply --manifest '.gradle/reservation-migration-demo/manifest.enc.json' --approve-manifest $report.manifestHash --operator 'synthetic-operator' --maintenance-evidence 'synthetic-freeze-ticket' --max-units 1
node tools/reservation-migration/cli.mjs --project demo-autocare-migration --database '(default)' --mode apply --manifest '.gradle/reservation-migration-demo/manifest.enc.json' --approve-manifest $report.manifestHash --operator 'synthetic-operator' --maintenance-evidence 'synthetic-freeze-ticket'
node tools/reservation-migration/cli.mjs --project demo-autocare-migration --database '(default)' --mode rollback --manifest '.gradle/reservation-migration-demo/manifest.enc.json' --approve-manifest $report.manifestHash --operator 'synthetic-operator' --maintenance-evidence 'synthetic-recovery-ticket'
Remove-Item Env:AUTOCARE_MIGRATION_KEY
```

Retain a privately escrowed real rehearsal key before clearing it if recovery is
still needed. The public test fixture key is solely for automated synthetic tests.

For the existing authorization/concurrency suite, use its separate emulator command
and `node --test tests/firestore/reservations.test.mjs` from `RESERVATION_SECURITY.md`.
Do not point the old suite at the new project or change the new adapter to port 8787.

### Person 1 instance: future commands, not executed during target-isolation coding

Use a separate terminal and a new in-memory instance with no import arguments. Do
not stop, clear, seed or repoint the existing port-8788 instance. Confirm that port
8790 is unused and verify the new process/project/database and empty inventory before
seeding. Loading the local rule file below affects only this new emulator.

```powershell
Set-Location 'C:\Users\asus\Desktop\MAD Project\AutoCare_CarDetailingMarketplace'
& 'C:\Program Files\Android\Android Studio\jbr\bin\java.exe' -jar '.gradle/security-fix-before/firestore-emulator.jar' --host 127.0.0.1 --port 8790 --project_id demo-autocare-person1-migration --single_project_mode --single_project_mode_error --rules firestore.rules
```

After separate authorization to perform the synthetic dry run and after verifying
the new empty target, use a **new** private output directory. The seed command below
writes synthetic fixtures; it is not a unit-test command and was not executed in
the target-isolation coding step. Keep the fresh key privately for inspection/recovery.

```powershell
Set-Location 'C:\Users\asus\Desktop\MAD Project\AutoCare_CarDetailingMarketplace'
$env:FIRESTORE_EMULATOR_HOST = '127.0.0.1:8790'
$env:AUTOCARE_MIGRATION_KEY = & node -e 'process.stdout.write(require("node:crypto").randomBytes(32).toString("hex"))'
node tests/firestore/migration-example.mjs --project demo-autocare-person1-migration --database '(default)' --output-dir '.gradle/reservation-migration-person1'
node tools/reservation-migration/cli.mjs --project demo-autocare-person1-migration --database '(default)' --mode dry-run --decisions '.gradle/reservation-migration-person1/synthetic-decisions.json' --output '.gradle/reservation-migration-person1/manifest.enc.json'
node tools/reservation-migration/cli.mjs --project demo-autocare-person1-migration --database '(default)' --mode inspect --manifest '.gradle/reservation-migration-person1/manifest.enc.json'
```

`migration-example.mjs` uses the same validated adapter and selected fixture writer;
it still refuses a nonempty inventory/policy and refuses existing decision files.
The existing emulator integration suite remains explicitly tied to port 8788 and its
original disposable project. It is not parameterized to reset Person 1's database.
Offline unit tests for both targets require no running emulator and perform no
seeding, migration application or rollback:

```powershell
node --test tests/firestore/migration-plan.test.mjs tests/firestore/migration-targets.test.mjs
git diff --check
```

## Emulator, staging and live verification plan

### Emulator acceptance

- [ ] Pure planner: fixed Kotlin ID vector; India-midnight conversion; process timezone
  independence; malformed dates/times; deterministic output; protected references.
- [ ] Legacy/canonical/terminal records; available and reserved aliases; unexpected
  public PII; missing dependencies and foreign ownership; orphan and cancelled slots.
- [ ] Competing target claims; existing target collision; multiple slots; explicit
  missing-slot and retirement approval; no automatic deletion or guessed dates.
- [ ] Dry-run/default execution/CLI create no DB or audit writes; artifacts cannot be
  overwritten; wrong environment and altered/encrypted manifests are rejected.
- [ ] Real REST precondition rejection; dependency/inventory changes; interruptions;
  lost acknowledgments; idempotent reruns; bounded units and partial-run journal.
- [ ] Rollback restoration/quarantine, unrelated later records, changed dependencies,
  post-cutover edits, ABA changes and interrupted rollback.
- [ ] Slot-only/booking-only units retain unchanged partner versions; owner confirmation
  rejects rollback without partial pair restoration; interrupted rollback rechecks
  partners in both pending and finished units.
- [ ] Client audit/policy access denied; actual gate remains disabled throughout every
  scenario. Enabled-policy refusal uses a simulated read, never a policy write.

### Isolated cloud staging: blocked pending preparation

The commands above can be repeated on a separate test machine with a disposable
local emulator. They are the exact supported staging-of-the-tool commands today.
There is intentionally **no cloud staging apply command**: an unknown staging project
is refused before any request. Do not substitute a staging ID into these commands
expecting a cloud connection.

Before providing cloud commands, authorize a specific project/database, implement and
review a separate allowlisted adapter, choose operator/IAM identities, implement and
test the freeze, approve historical/retirement policies and restoration procedures,
and define the full-data scale/limits, export/subcollection coverage and monitoring.
Then supply exact reviewed commands tied to that environment, without committed secrets.

The future staging matrix must repeat every emulator invariant plus actual IAM,
backup/restore, maintenance rejection with queued/offline clients, real-device
booking/cancellation in different timezones, full inventory reconciliation, duration,
restart and rollback-under-load measurements. Use approved synthetic data first.

### Future live acceptance: separately authorized, not executed

- [ ] Staging evidence, administrator decisions and operational runbook approved.
- [ ] Correct project/database/identities and reviewed deployed versions verified.
- [ ] Freeze proven; consistent backup and tested restoration available.
- [ ] No unresolved manifest blockers; signed-off batches and private journal complete.
- [ ] Full data invariants and exception classifications accepted.
- [ ] Completion flag enabled only by named administrator after evidence approval.
- [ ] Approved smoke tests, monitoring/support and recovery ownership confirmed.
- [ ] Audit/backup/key retention and post-cutover reconciliation complete.

No local test result should be described as successful production deployment,
production migration, production IAM validation or a completed cloud staging rehearsal.

## Original preparation verification — 28 October 2026 (before guard correction)

| Executed check | Observed result |
| --- | --- |
| `node --test tests/firestore/migration-plan.test.mjs tests/firestore/migration-emulator.test.mjs` | 44 reported passing tests: 16 pure-planner tests, 27 emulator scenarios and one parent wrapper; zero failures/skips |
| `node --test tests/firestore/reservations.test.mjs` | 33 reported passing tests: 32 existing authorization/transaction scenarios and one parent wrapper; zero failures/skips |
| Documented empty-emulator synthetic seed, default CLI dry-run and redacted inspection | One unit, two proposed writes, zero blockers; the migration CLI did not apply them |
| `git diff --check` | Passed; existing staged authentication test retained |

The test suites exercised apply/rollback against synthetic records only. The manual
example seeded synthetic fixtures explicitly and used only dry-run/inspect for the
migration utility. Android build/lint/unit tasks were not rerun for these Node/docs
changes. Local emulator connectivity required the execution environment's local-network
permission. Both emulator processes started for this preparation were stopped afterward.

Verdict: ready for isolated **emulator** rehearsal within the stated limits. Cloud
staging and live migration require the missing operational decisions, separately
reviewed adapter, freeze implementation and demonstrated restoration/recovery evidence.

## Rollback pair-guard correction — 28 October 2026

Root cause: filtering unchanged writes also removed them from recovery checks, and
the dependency list covered workshop/customer/service/vehicle records rather than
the complete booking/slot pair. A slot-only normalization therefore lacked a rollback
guard on its unchanged booking. Manifest format 2 now retains the complete pair
read set independently of the filtered write set, as described above.

Observed focused commands from the repository root:

```powershell
node --test tests/firestore/migration-plan.test.mjs
node --test tests/firestore/migration-plan.test.mjs tests/firestore/migration-emulator.test.mjs
```

The first command passed 20 planner tests. The combined command passed **58 reported
tests: 20 planner tests, 37 emulator scenarios and one parent wrapper**, with zero
failures, cancellations or skips. This includes four new planner tests and ten new
emulator scenarios for unchanged partners, the exact owner-confirmation regression,
both write directions, version-preserving normal rollback, ABA activity, changes
after preflight, interrupted application/recovery, and changes to unrelated records.

The existing listener on `127.0.0.1:8788` was verified as the requested
`demo-autocare-migration` emulator, containing synthetic fixtures with a disabled gate,
before the disposable test suite was run. That pre-existing emulator was reused and
left running. No migration/rollback CLI was executed outside the synthetic tests.
The actual rollout flag stayed false throughout every scenario; enabled-policy
refusal is tested with a simulated read. No cloud connection, rule deployment,
Firebase setting change, Android/rule source change, staging/commit/branch operation,
or production migration/rollback occurred. Existing staged work was retained.

Isolated emulator dry-run testing may proceed with freshly generated format-2
manifests. The operational freeze, backup/restoration, administrator authorization,
retention/key-custody and cloud-adapter limitations above remain in force. Recovery
can still be partially complete across units when later activity causes a stop;
every individual pair's recovery is guarded and atomic.

## Two-target isolation coding verification — 28 October 2026

Executed only offline unit tests and source/diff checks:

```powershell
node --test tests/firestore/migration-plan.test.mjs tests/firestore/migration-targets.test.mjs
git diff --check
```

Observed: **39 passed, zero failures/skips** (20 existing planner tests and 19
target-isolation tests). The target tests stub transport; child CLI inspection uses
a preload that exits distinctly if any network operation is attempted. Coverage
includes both approved tuples, immutable unchanged original defaults, arbitrary and
mixed-target rejection, selected read/write/fixture namespaces, manifest binding,
cross-target apply/rollback/resume refusal before adapter reads, and CLI inspection
with matching or stale host environments. Cross-target execution tests exercise
refusal only: they perform no migration or rollback. Fixture-write tests serialize
synthetic values only; they do not seed a database.

The final changes were compared against task-start source copies because these
migration files already contained untracked work. Git and changed-file whitespace
checks passed. No emulator was started or contacted, no existing emulator data was
read/reset/changed, and no seed/dry-run/apply/rollback workflow was executed. Android
source, Firestore rules, staged changes and Git history were preserved.

The earlier 58-test emulator result is historical and was not repeated during this
coding-only step. Person 1 startup, live local endpoint/empty-inventory verification,
fixture seeding and the actual synthetic dry run remain separate later steps.

## Person 1 synthetic dry run and read-only integration — 28 October 2026

The documented JAR command was run only for `127.0.0.1:8790`, project
`demo-autocare-person1-migration`, database `(default)`, without an import. Listener
identity and real API requests confirmed this exact target. Before setup,
`listCollectionIds` returned no root collections, the scoped snapshot had zero
documents, and the policy was absent. No data was deleted or cleared.

The documented seed command created eight synthetic documents: one booking, one
slot, one workshop, one service, one vehicle, two user profiles and the disabled
rollout policy. A new private output directory was used:

```text
.gradle/reservation-migration-person1-20261028-dea74aa7/
```

Actual CLI commands, after setting `FIRESTORE_EMULATOR_HOST=127.0.0.1:8790` and a
fresh private `AUTOCARE_MIGRATION_KEY`:

```powershell
node tests/firestore/migration-example.mjs --project demo-autocare-person1-migration --database '(default)' --output-dir '.gradle/reservation-migration-person1-20261028-dea74aa7'
node tools/reservation-migration/cli.mjs --project demo-autocare-person1-migration --database '(default)' --mode dry-run --decisions '.gradle/reservation-migration-person1-20261028-dea74aa7/synthetic-decisions.json' --output '.gradle/reservation-migration-person1-20261028-dea74aa7/manifest.enc.json'
node tools/reservation-migration/cli.mjs --project demo-autocare-person1-migration --database '(default)' --mode inspect --manifest '.gradle/reservation-migration-person1-20261028-dea74aa7/manifest.enc.json'
```

Observed: one unit, **two proposed writes, zero blockers and zero warnings**.
The authenticated manifest is format 2 and includes the Person 1 endpoint, project
and database. Its digest is
`eb6e186b1de00d757c5243ad251fba229ae6b645fc3f3d457edafca28bcffabf`.
The private key was not logged or stored in plaintext: `manifest-key.dpapi` contains
Windows user-bound protected key material, with recovery verified locally.
`setup-snapshot.enc.json` is the encrypted real snapshot captured immediately after
fixture setup and before CLI planning. Retain these artifacts privately; this key
escrow requires the same Windows user/environment and is not portable backup evidence.

The original `migration-emulator.test.mjs` was deliberately excluded: it targets
8788, clears the database and applies/rolls back migrations, which were prohibited
for this run. The new `migration-person1-readonly.test.mjs` instead uses only Person
1, requires the existing artifacts, and rejects every document commit at its
transport boundary. Read-only transaction cleanup is permitted; no migration
rollback is called. It verifies the target, complete synthetic payloads, manifest
binding, proposed metadata, deterministic planning, dry-run defaults, disabled gate,
absent audit receipts and unchanged fixture field values/server versions.

For reproducible read-only verification in this Windows session, recover the key
only into the child-process environment, never into command output:

```powershell
Set-Location 'C:\Users\asus\Desktop\MAD Project\AutoCare_CarDetailingMarketplace'
$env:FIRESTORE_EMULATOR_HOST = '127.0.0.1:8790'
$env:AUTOCARE_PERSON1_ARTIFACT_DIR = (Resolve-Path '.gradle/reservation-migration-person1-20261028-dea74aa7').Path
$secure = Get-Content (Join-Path $env:AUTOCARE_PERSON1_ARTIFACT_DIR 'manifest-key.dpapi') -Raw | ConvertTo-SecureString
$env:AUTOCARE_MIGRATION_KEY = [System.Net.NetworkCredential]::new('', $secure).Password
try {
    node --test tests/firestore/migration-plan.test.mjs tests/firestore/migration-targets.test.mjs tests/firestore/migration-person1-readonly.test.mjs
} finally {
    Remove-Item Env:AUTOCARE_MIGRATION_KEY
}
git diff --check
```

Observed: **48 reported tests passed, zero failures/skips** (20 planner tests,
19 target-isolation tests, eight real-emulator read-only checks and one parent
wrapper). The final snapshot still exactly matched the post-setup encrypted
baseline, including update versions. Git and changed-file whitespace checks passed.
The old 58-test write/rollback integration result remains historical; those mutation
scenarios were not repeated and are not covered by the read-only result.

Only the documented eight-document fixture commit wrote to a database. No further
document writes, deletes, clears, migration application, gate enablement or migration
rollback occurred. The existing emulator on 8788 was not contacted. No cloud Firebase,
Android/rule source, Firebase settings, staged work or Git history was changed.
Person 1's emulator was left running with the synthetic setup intact for inspection.
