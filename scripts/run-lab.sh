#!/usr/bin/env bash
set -euo pipefail
if [[ $# != 2 ]]; then
  echo 'Usage: scripts/run-lab.sh SERIAL ACTION' >&2
  exit 2
fi
serial=$1
action=$2
case "$action" in
  probe|create_fixture|rename_fixture|capture_fixture|export_fixture|open_fixture|write_fixture|disable_fixture|create_ink|capture_ink|export_ink|open_ink|stage_ink|stage_ink_interrupt|disable_ink|image_ink|cluster_snapshot|cluster_baseline|cluster_update|cluster_switch|cluster_restore) ;;
  *) echo "Unknown lab action: $action" >&2; exit 2 ;;
esac
# Restart only our app so debug intents reliably run on current BOOX firmware.
adb -s "$serial" shell am force-stop dev.aragonite.powersync
adb -s "$serial" shell am start -n dev.aragonite.powersync/.MainActivity --es lab_action "$action"
