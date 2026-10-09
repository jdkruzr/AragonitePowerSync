#!/usr/bin/env bash
set -euo pipefail
if [[ $# != 2 ]]; then
  echo 'Usage: scripts/run-lab.sh SERIAL ACTION' >&2
  exit 2
fi
serial=$1
action=$2
case "$action" in
  create_rich|capture_rich|export_rich|stage_rich|image_rich|open_rich) ;;
  conflict_capture|conflict_edit|conflict_remove|conflict_restore|endpoint_status) ;;
  v2_work_insert|v2_work_capture|v2_work_remove) ;;
  library_inventory|library_export_one) ;;
  reader_history_enroll|reader_history_capture|reader_history_export|reader_history_query|reader_history_start|reader_history_pull|reader_history_full) ;;
  reader_config_refresh|reader_fresh_restart|reader_fresh_publish_config|reader_fresh_auto_sync|reader_fresh_capture|reader_fresh_export|reader_fresh_push|reader_fresh_pull_full|reader_fresh_pull|reader_fresh_query_metadata|reader_fresh_query|reader_fresh_restart_job|reader_fresh_requeue|reader_fresh_start|reader_pdf_pull_full_source|reader_pdf_push_peer|reader_pdf_pull_source|reader_pdf_stage_peer_metadata|reader_pdf_start|reader_pdf_enroll_peer|reader_pdf_restore_owned|reader_pdf_publish_metadata|reader_pdf_query_metadata|reader_pdf_query|reader_pdf_restart|reader_pdf_sync_source|reader_pdf_sync_peer|reader_pdf_export_managed|reader_pdf_upload|reader_pdf_download|reader_pdf_export|reader_pdf_capture|reader_fixture_capture|reader_library_guard|reader_fixture_pull_full|reader_fixture_annotations) ;;
  reader_fixture_cloud_start|reader_fixture_cloud_restart|reader_fixture_query_cloud_metadata|reader_fixture_cloud_capture|reader_fixture_conflict_edit|reader_fixture_conflict_publish|reader_fixture_cloud_comment|reader_fixture_library_delta|reader_fixture_library_sync|reader_fixture_book_prepare|reader_fixture_book_upload|reader_fixture_book_download) ;;
  reader_fixture_publish|reader_fixture_start|reader_fixture_restore|reader_fixture_bookmarks|reader_fixture_bookmark_revision|reader_fixture_identity_earlier|reader_fixture_identity_restore) ;;
  endpoint_provision|endpoint_provision_marker|endpoint_reassert|endpoint_enable|endpoint_flags_restore|endpoint_restore) ;;
  cluster_uid_probe) ;;
  richtext_recovery_apply|editor_quit_arm|editor_noop_arm|editor_switch_arm|editor_same_arm|editor_switch_foreground) ;;
  gateway_tree_query|gateway_selected_enable|gateway_restart_tree|gateway_selected_capture|gateway_shared_open|gateway_shared_pull|gateway_shared_export|gateway_shared_sync|gateway_shared_capture|gateway_shared_rename) ;;
  account_baseline|account_restore|account_lab_info) ;;
  gateway_create|gateway_capture|gateway_enable|gateway_disable|gateway_rename|gateway_preservation) ;;
  native_restored_wake|catalog_restore_original|probe|create_fixture|rename_fixture|capture_fixture|export_fixture|open_fixture|write_fixture|disable_fixture|create_ink|capture_ink|export_ink|open_ink|stage_ink|stage_ink_interrupt|disable_ink|image_ink|cluster_snapshot|cluster_baseline|cluster_update|cluster_switch|cluster_restore|account_login_lab) ;;
  *) echo "Unknown lab action: $action" >&2; exit 2 ;;
esac
# Restart only our app so debug intents reliably run on current BOOX firmware.
adb -s "$serial" shell am force-stop dev.aragonite.powersync
adb -s "$serial" shell am start -n dev.aragonite.powersync/.MainActivity --es lab_action "$action"
