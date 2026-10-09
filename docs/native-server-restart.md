# Native server restart during pending notebook work, 2026-10-07

This experiment uses only the owned disposable text notebook on Palma 2 Pro and
Go 10.3 II Lumi. Both devices use the LAN facade and native Couchbase Lite; no
custom replication engine or device APK patch is involved.

## Restart boundary

A device/fixture-scoped hold intercepts a successful signed HTML PUT only after
the Gateway attachment has been stored. The journal is fsynced before withholding
the HTTP response. The marker-bearing object is escrowed by exact backend document
ID/revision, and its SHA-256 checked before interruption. The NOTE_TREE commit
record is unchanged at this boundary.

The facade is killed before the bounded acknowledgement hold expires. Sync
Gateway and Couchbase Server are then stopped and restarted with their existing
persistent volumes. The facade remains down until the exact held attachment has
been read back from the restarted backend and compared byte for byte. Native
ksync processes and the BLIP proxy remain running through the outage.

Two runs confirm exact held-byte survival and stable device session IDs. Native
ksync PIDs are unchanged across each actual outage. A source-only native restart
was used during setup to make upload scheduling reach the hold; it is separate
from the untouched post-restart observation window.

## Clean repeat

The second run restarts the facade in its own persistent execution session.
During 91.4 seconds of observation, every sampled facade listener is healthy,
backend snapshots remain readable, both native daemon PIDs stay unchanged, and
no native recovery trigger is requested. The NOTE_TREE commit revision does not
advance. A read-only receiver file check finds a small placeholder without the
new marker. The source native export retains the exact interrupted asset.

The receiver export after the untouched window has no marker. Opening the native
receiver editor and waiting does not immediately apply it. A subsequent explicit
receiver tree restart, scoped sync, open and export still has no marker. At this
stage both devices have exactly one native row for the notebook, one page, and
matching native page identities; this is scoped duplicate prevention evidence,
not proof against every duplicate resource/history record.

The pending NOTE_TREE record has `recordType=1`, `commitType=1`, `commitStatus=1`.
A server record or checkpoint is not a complete notebook acknowledgement. Keep
metadata receipt, asset bytes, native export application and editor display as
separate checks. The initial run's commit advance does not by itself establish
that its interrupted content was completely republished.

## Explicit source recovery and active-content limit

A source-only scoped sync after the clean window also fails to advance the commit
within a separate 30-second observation. Next, the source daemon is restarted,
the native editor opened, a small new edit made, and the note closed. Its commit
advances, but no acknowledged PUT in that attempt contains the interrupted marker,
and the receiver export still lacks that marker.

The inspected source screenshot before this new edit shows older active content:
the interrupted marker is absent. Its exact bytes were present in the prior
native archive, but that archive includes historical/unreferenced HTML as well
as active resources. Thus the attempted new edit was made to older active text;
it is not successful resubmission of the interrupted content. This correction is
explicit in the result ledger. We did not inject the escrowed body back into the
device to manufacture a native recovery pass.

These observations occur after explicit pull/restart operations. They do not
prove that the backend restart alone replaced the active buffer, nor identify the
specific operation that selected the older resource. Full automatic publication,
active-content preservation and a reliable source recovery sequence remain open.
The interrupted revision is retained in private escrow and earlier exports.

One BLIP 101 upgrade per device occurred after backend readiness and before any
explicit recovery trigger, before the first listener sample. Further upgrades
occur during explicit recovery. This establishes reconnection for at least one
replication connection on each device, not every notebook/MESSAGE scope or exact
checkpoint cursor continuation.

## First-run measurement correction

The first driver restarted the facade as its child. It survived long enough for
a commit revision to advance roughly 2.3 seconds after readiness, but exited when
the experiment driver ended. Later native logs show connection refusals. Thus
later scoped retry, unchanged reopen/close and fresh-process failures in that run
are confounded by facade downtime and cannot establish a ksync recovery defect.
All original measurements and failed assertions remain in the evidence.

A later receiver export from that run contains the exact original held asset,
matching one earlier signed GET. This shows retained-content delivery, but its
application timing is not a clean unattended-recovery measurement. The second
run exists specifically to separate facade supervision from native behavior.

## Checkpoints and implementation implications

The bounded recovery trace records getCheckpoint (2), setCheckpoint (7),
subChanges (2), proposeChanges (4), changes (5), and rev (4). It begins after the
first backend restart, during explicit recovery investigation. It does not
capture the first reconnect checkpoint negotiation or prove exact cursor resume,
zero metadata replay, or restart of every possible one-shot replication scope.
Gateway console verbosity was restored after capture.

Use durable object storage and retain authentication/session state across server
restarts. A successful durable object write must survive loss of its response.
Keep native conflict winners and stable device identities. Provide observable
pending/recovery state; neither an existing stored object nor a changed commit
record justifies declaring the notebook fully applied. A bounded negative retry
window does not prove that no later scheduler trigger can recover it.

This run kills the facade, but gracefully stops the two backend containers. It
is not a Couchbase hard-crash, host power-loss, backup-restore, TLS outage, reading
asset, multi-node failover, or exhaustive checkpoint qualification.

Private evidence: `artifacts/gateway-lab/qualification/server-restart/`, with
clean repeat in `round2/`. It contains exact held/readback bytes, exports,
server snapshots, native captures/screenshots, session/PID comparisons, listener
samples, driver sources/logs and a bounded private Gateway trace. No Android or
production Server implementation changed; no fresh build is claimed for this
experiment using the already-installed probe.

## Closure

Both devices are restored to their exact original account/cluster/sync settings.
Original notebook preservation guards and complete original reader metadata
checks pass. Endpoint markers and host overrides are removed; synthetic reader
aliases remain disabled. Gateway policy and console logging are restored. All
lab services are stopped, local lab ports are closed, and persistent volumes
are retained. The private SHA-256 manifest verifies 222 evidence files.

## Explicit recovery continuation

[Same-note rich-text recovery](native-richtext-recovery.md) now restores the
interrupted body through an explicit fresh resource/shape revision, with native
publication and active content on both devices. New reference analysis also
shows that the earlier source archive referenced the interrupted body, and that
pre-open exports can omit the reference even when later native open succeeds.
Keep rendered/post-save references separate from incomplete export observations.
The earlier bounded unattended failure remains valid; this is assisted recovery,
not proof of automatic retry or the precise operation that selected older text.
