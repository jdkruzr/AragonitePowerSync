# PowerSync icon

The approved amber aragonite and teal sync-arrow artwork is the product icon for
`dev.aragonite.powersync.client`. The diagnostic probe keeps its existing identity.

`powersync-aragonite-sync-v2.png` is the current transparent artwork, composed
with the shared `aragonite-crystal-master-v1.png` as its primary reference and the
original teal sync arrows as its layout reference. Both the master and generation
prompt are retained here. This is a generative composition, not pixel-exact layering.
`powersync-aragonite-sync-concept-v1.png` and its prompt remain as the original.

Run `python3 scripts/export-launcher-icons.py` (requires Pillow) from the repository
root to copy the current artwork into the product's `drawable-nodpi` resources and
regenerate all legacy launcher sizes. The adaptive icon uses a white background
and a one-sixth inset to keep the full arrows inside launcher masks; themed icons
use its alpha silhouette. Legacy launcher PNGs are flattened onto white at
48, 72, 96, 144 and 192 pixels.

The original icon shipped in release 0.1.1 (version code 2). The shared-crystal
refresh is staged for the next release; no version bump or device installation is
part of this artwork update.
