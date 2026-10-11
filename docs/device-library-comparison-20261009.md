# Production notebook-library comparison — 2026-10-09

## Verdict

The two current-account notebook libraries are closely converged, but not byte-identical: one shared injection-test shape archive contains a different text field on Palma. No other differences were found in the compared non-stash shared notebook resources, except a Lumi-only tag serialization whose native database record matches Palma. This is a read-only comparison, not a repair or full fresh-device restore qualification.

## Capture and scope

Devices: Go 10.3 II Lumi and Palma 2 Pro. Current account selected from production `boox_identity`; native `NoteModel` and `TagModel` queried over ADB. Server notebook projection and live blob bindings captured from production PostgreSQL. Native `.ksync/document` and `.ksync/point` regular files hashed with SHA-256. Scope for resource comparison: notebooks/folders live and sync-enabled on both devices. Other-account records, deleted notes and disabled notes are not silently included in that scope.

Private evidence and scripts: `artifacts/library-comparison-20261009/` (ignored). Captures include provider snapshots, normalized hashes, decoded discrepancy bodies, detailed difference lists, and a final metadata recheck. No sync triggers, device writes, cache clearing, account changes or restarts were performed. A first Android `find -exec ... +` hash attempt exceeded its argument limit; its incomplete output was discarded. The replacement `xargs -0 -n 40 sha256sum` captures both completed with exit 0 and empty error files.

## Metadata

- Lumi: 1,523 current-account records, 1,082 live; 1,080 live and sync-enabled.
- Palma: 1,524 records, 1,083 live; 1,081 live and sync-enabled.
- Intersection live and enabled on both: **1,076 notebooks and four folders**.
- Shared titles, parent placement, notebook kind/scene, association fields and declared page lists match. Empty and null optional fields normalized together.
- Each device's live sync-enabled notebook IDs, live status and declared page lists agree with production server projection.
- Four peer record differences all involve sync disabled on at least one side: two opposite live/deleted states, one Palma-only disabled text notebook, and one shared text fixture disabled only on Lumi. These are not grounds to force-delete or upload anything.
- All captured `asyncStatus` values are zero. This alone is not evidence of an empty daemon queue.
- Final native notebook metadata captures are byte-identical to the opening snapshots; shared titles also match.
- Declared page lists contain 1,665 entries in the common enabled scope. This is a metadata count, not an independently decoded full virtual-page count.

## Resource comparison

Hashed 67,917 files on Lumi and 64,209 on Palma, including records outside the common enabled scope. Within that scope:

- **32,362 resource files match exactly**: 13,496 shape archives, 11,817 point files, 2,875 virtual records, 1,471 page-model files, 2,301 template files, 319 note resources, 73 resource files, nine TOC files, one link file.
- One differing shape archive, in `20260418 Injection Test`. Both ZIPs pass CRC. Both contain the same 18 shape IDs. Field-by-field protobuf comparison finds only two changes on one shape: `updatedAt` and text `INJECTED-MARKER-001` present on Palma but absent on Lumi. All other parsed fields match. Server's recorded blob hash matches Lumi's archive. No repair attempted; retaining the native alternatives is preferable to guessing which to overwrite.
- One tag protobuf file exists only on Lumi. All **97 native tag database records**, including content, notebook/page association and status, match across devices. The tag represented by the extra serialization exists and is enabled on both. Import timestamps differ, as expected for separately imported native rows. Thus file presence alone would have produced a false missing-tag report.
- Stash differences: 26,479 Lumi-only and 22,895 Palma-only paths. These are kept separate from non-stash content; their semantic/history equivalence was not audited.

## Server comparison and limits

Server live blob hashes match 32,357 compared Lumi files and 32,356 Palma files. The six other common resource differences are MP3/PCM files: both devices agree byte-for-byte with each other, but server stored-object hashes differ. Previous work identified audio transport/container handling; this audit does not independently unwrap and validate these six server objects, so it does not certify their server round trip.

The server also has 1,071 notebook-root cover PNG bindings absent from both scanned shared directories. Thumbnail/cache locations were not enumerated, so these are not classified as missing user page content.

This establishes notebook catalog and extensive native resource convergence, with a specific retained test-content discrepancy. It does not prove every native UI renders every page, that all transitive references resolve, that a fresh device can restore the full library, that native daemon queues are empty, or that Reader ebook files/annotations are synchronized. No global fully-synced badge should be inferred from catalog equality or `asyncStatus=0` alone.
