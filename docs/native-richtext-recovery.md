# Explicit native rich-text recovery, 2026-10-07

The interrupted `RST350b25cc` text from the clean server-restart run is restored
under the original notebook, rich-text page and shape IDs. Lumi displays it,
accepts a native edit, publishes marker-bearing assets through stock ksync, and
Palma displays the resulting content. Both devices retain the recovered marker
and native edit through a cold Notes reopen. This is an ordinary-app-assisted,
text-only lab recovery prototype, not unattended recovery or a production
server-only restore endpoint.

## Recovery construction and application

The host selects an exact retained body from the previous private escrow, and a
fresh native export providing the current shape/point/resource context. The
builder [make-richtext-recovery.py](../scripts/make-richtext-recovery.py) retains
the current shape ID, page ID, geometry/point payload and unknown wire fields.
It creates fresh resource, resource revision, shape revision and point revision
IDs with a newer timestamp. The recovered HTML/JSON bytes are copied exactly;
old immutable assets are not rewritten. Shape field 14 and the resource PB both
reference the fresh HTML. This four-entry closed payload includes the resource,
its PB metadata, points and shape ZIP.

The ordinary-UID [native recovery handler](../app/src/main/java/dev/aragonite/powersync/NativeRichTextRecoveryLab.java)
requires the owned live lab fixture, selected notebook, paused note sync, one
mapped rich-text page, matching shape/resource references and body/payload SHA-256.
It compares the current referenced resource ID and active body SHA-256 to the
prepared baseline. All payload validation happens before shared-file staging.
Private immutable receipts and an application journal record preparation,
staging and the native import request. Resource bytes are staged before PB,
points and shape publication; the existing scoped native data-update interface
requests import. No broad RESTORE_NOTE or root operation is used.

The guard is a prepared-state check in this paused/idle experiment, not an atomic
lock against arbitrary concurrent native editing. Dirty-buffer coordination
remains a separate qualification gate.

## Native results

| Observation | Result |
| --- | --- |
| Lumi first native open | Interrupted marker visibly restored |
| Referenced asset after native open/save | Exact bytes equal the retained escrow |
| Native edit after recovery | Adds ` RECOVERED` and retains all original text |
| Native publication | Commit advances; successful signed native PUT contains marker |
| Palma delivery | Successful signed GET, visible editor and active referenced export |
| Identities | Original note/page/shape IDs on both; one notebook and one page |
| Final content/style | Content equal after deduplicating identical marks; style preserved |
| Cold reopen | Marker and native edit remain active on both devices |
| Stale recovery request | Refused with active-text-changed error after the native edit |
| Duplicate recovery request | `alreadyApplied`; no second native import requested |

The replay test uses a fresh recovery payload containing the exact current text,
so it changes no text. After native open/save hydrates the resource reference,
the same request returns `alreadyApplied`. This does not qualify replay into an
unloaded/incomplete native model or across every interrupted application phase.

Native save/export can produce new resource IDs and duplicate identical rich-text
marks. Final active asset hashes differ between devices, while normalized content
and contentStyle match. Receiver archives separately retain exact marker-bearing
signed-download bytes. Do not label normalized equality as raw byte equality.

## Reference inspection correction

The older source export from the restart experiment has a rich-text shape whose
field-14 resource reference points at the exact interrupted body. Receiver
exports from that investigation reference older bodies. Later fresh exports can
omit field 14 entirely: Palma did so before this recovery, and Lumi did so just
after import but before opening its native editor. The latter editor then
rendered the restored body, and its next saved export included the exact active
reference. Thus absence in a pre-open export is an incomplete observation, not
proof that the live editor cannot load the text or that the resource is orphaned.

The builder refuses an incomplete current reference. Recovery used Lumi's valid
current reference, then native replication delivered to Palma. The archived
reference timeline, editor screenshots and post-save references establish the
actual recovery result; they do not identify which earlier pull/cache/save step
selected the older text in the restart experiment.

## Facade / Alexandria implications

Keep the native winner as normal live state. A user-selected recovery is a new
ordinary edit based on explicitly chosen retained bytes, with current-state
preconditions, fresh native references and durable provenance. It must not
silently reactivate arbitrary history or resurrect a removed notebook. Report
receipt, native application, active-content verification and peer application
separately. Recovering the body alone is insufficient if its shape points at
another resource.

This prototype imports through the helper app on Lumi; stock ksync performs the
subsequent server publication and Palma delivery. It does not establish that a
server can publish the four assets alone and reliably trigger application without
the helper. A production owner-authenticated recovery API/UI, server-only
publication, automatic orphan/losing-intent collection, broader content types,
concurrent edit coordination and replay across every failure phase remain open.
The existing optional-history policy is unchanged.

## Validation and evidence

Fresh JDK 17 assembleDebug/lintDebug pass; the probe is installed in place on both
devices. Three focused tests cover exact body preservation, stable identity and
point data, unknown fields, fresh-output refusal, unsupported attachment graphs,
ambiguous shapes and incomplete references. All 34 Python tests pass. Native
Notes and ksync APKs are unmodified.

Private evidence: `artifacts/gateway-lab/qualification/richtext-recovery/` includes
current/restored/published/cold native exports, exact retained body, recovery
payloads and receipts, native application journals, asset escrow, signed transfer
receipts, screenshots, freshness/replay results, full preservation guards and
build/test/install logs. Earlier failed assertions are retained; in particular,
the first pre-open export lacked a reference even though the subsequent editor
rendered the restored content.

## Closure

Both devices were restored to their original endpoints, account state and sync
flags. All 1,517 guarded original notebook rows on each device and the complete
original reader metadata (28 Palma, 14 Lumi rows) pass preservation checks;
synthetic reading aliases are disabled. Temporary recovery requests and shared
payloads were removed. The owned LAN facade/proxy and backend containers are
stopped; persistent lab volumes remain available.

The private bundle contains 123 checksummed files, independently reverified after
closure. This qualifies the explicit helper-assisted text path described above;
it does not close the automatic server-only recovery gate.
