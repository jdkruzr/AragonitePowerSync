# Offline full-body Reader alternatives archive

2026-10-08. Read-only lab archive for one owned Reader book; not a production archive service. Evidence: `artifacts/gateway-lab/qualification/reader-offline-alternative-archive/`.

Thirty stored native shape ZIPs decode to 40 records. Five shape IDs have opposing status histories, containing 24 operations. These are status-disagreement candidates; sequential erase/Undo can produce them, so concurrency or a preferred winner is not inferred.

Each candidate packet retains the complete original protobuf wire record (including unknown fields), its byte hash, full original ZIP body and enclosing member, original server object identity/revision/key, and the exact referenced point/pressure object. All candidate pressure dependencies resolve and all stored bodies verify against server size/SHA256. Current owned mode-7/commit context is retained separately. Duplicate occurrences of an information revision must have identical full wire bytes. The archive uses content-addressed bodies with explicit references, allowing shared dependencies without dropping competing operations.

An independent verifier checks all 24 wire records against their original ZIP members and verifies every referenced body. Temporary-copy controls remove or corrupt a pressure object; both are rejected. Original archive objects remain untouched. Full server snapshots before/after are identical. No device actions, live listing change, winner selection or automatic expiry/purge occur.

This qualifies the narrow full-body plus pressure archive representation. It does not define durable production storage, retention duration, garbage collection, restore/UI workflows, non-pressure resource closure, arbitrary note schemas or proof that Sync Gateway revision history is an archive. These alternatives are offline artifact packets: putting their historical ZIPs into a native live listing is not an archive and can revive or disable shapes, as prior history tests showed.

Alexandria can preserve original competing operations and dependencies outside the native feed, later presenting an explicit selected-state repair if the user asks. Retention and deletion must account for dependency references and native checkpoints; no automatic reapplication follows from this archive. Both archive and verifier drivers are retained with checksum evidence.
