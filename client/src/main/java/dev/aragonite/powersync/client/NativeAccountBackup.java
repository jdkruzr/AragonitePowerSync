package dev.aragonite.powersync.client;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.*;

/** Credentials stay in PrivateState on this device; never send or log these rows. */
final class NativeAccountBackup {
 private final NativeBridge bridge;
 NativeAccountBackup(NativeBridge bridge) { this.bridge=bridge; }

 private JSONArray rows(String selection,String... args)throws Exception {
  JSONArray rows=new JSONArray();
  try(Cursor c=bridge.context.getContentResolver().query(NativeBridge.ACCOUNTS,null,selection,args,null)) {
   if(c==null)throw new UserError(R.string.error_native_account_cache_unavailable);
   while(c.moveToNext()) {
    if(rows.length()>=32)throw new UserError(R.string.error_unexpected_native_account_cache_size);
    JSONObject row=new JSONObject();
    for(int i=0;i<c.getColumnCount();i++) {
     Object value=switch(c.getType(i)) {
      case Cursor.FIELD_TYPE_NULL -> JSONObject.NULL;
      case Cursor.FIELD_TYPE_INTEGER -> c.getLong(i);
      case Cursor.FIELD_TYPE_STRING -> c.getString(i);
      default -> throw new UserError(R.string.error_unsupported_account_cache_field);
     };
     row.put(c.getColumnName(i),value);
    }
    rows.put(row);
   }
  }
  return rows;
 }

 private static List<Map<String,Object>> maps(JSONArray rows)throws Exception {
  List<Map<String,Object>> result=new ArrayList<>();
  for(int i=0;i<rows.length();i++) {
   JSONObject row=rows.getJSONObject(i);Map<String,Object> map=new HashMap<>();
   for(Iterator<String> keys=row.keys();keys.hasNext();) {
    String key=keys.next();Object value=row.get(key);
    map.put(key,value==JSONObject.NULL?null:value instanceof Number?((Number)value).longValue():value);
   }
   result.add(map);
  }
  return result;
 }
 private static JSONObject json(Map<String,Object> row)throws Exception {
  JSONObject result=new JSONObject();
  for(Map.Entry<String,Object> entry:row.entrySet())result.put(entry.getKey(),entry.getValue()==null?JSONObject.NULL:entry.getValue());
  return result;
 }
 JSONArray capture(String uid)throws Exception {
  List<Map<String,Object>> saved=AccountCacheRecovery.capture(maps(rows(null)),uid);
  JSONObject active=bridge.account();
  if(active.optBoolean("loggedIn")&&uid.equals(active.optString("uid"))&&
     saved.stream().filter(row->uid.equals(row.get("uid"))&&active.optString("clusterId").equals(row.get("clusterId"))).count()!=1)
   throw new UserError(R.string.error_cannot_preserve_your_current_boox_account);
  JSONArray result=new JSONArray();for(Map<String,Object> row:saved)result.put(json(row));return result;
 }
 // Compare every column, including credentials, without putting values in errors.
 private static String selection(Map<String,Object> row,List<String> args)throws IOException {
  List<String> clauses=new ArrayList<>();
  for(String key:new TreeSet<>(row.keySet())) {
   if(!key.matches("[A-Za-z_][A-Za-z0-9_]*"))throw new UserError(R.string.error_unsupported_native_account_column);
   Object value=row.get(key);
   if(value==null)clauses.add(key+" IS NULL");
   else {clauses.add(key+"=?");args.add(value.toString());}
  }
  return String.join(" AND ",clauses);
 }
 void verifyBeforeLogin(JSONArray saved,String uid)throws Exception {
  AccountCacheRecovery.verifyBeforeLogin(maps(saved),maps(rows(null)),uid);
 }
 private static ContentValues values(JSONObject row)throws Exception {
  ContentValues values=new ContentValues();
  for(Iterator<String> it=row.keys();it.hasNext();) {
   String key=it.next();Object value=row.get(key);
   if(value==JSONObject.NULL)values.putNull(key);
   else if(value instanceof Number)values.put(key,((Number)value).longValue());
   else if(value instanceof String)values.put(key,(String)value);
   else throw new UserError(R.string.error_unsupported_saved_account_field);
  }
  return values;
 }

 boolean restore(JSONObject previous,JSONArray saved,String uid,String custom)throws Exception {
  JSONObject active=bridge.account();
  if(previous.optBoolean("loggedIn")&&active.optBoolean("loggedIn")&&
    previous.getString("uid").equals(active.optString("uid"))&&
    previous.getString("clusterId").equals(active.optString("clusterId"))) {
   for(Map<String,Object> old:maps(saved)) {
    if(previous.getString("uid").equals(old.get("uid"))&&previous.getString("clusterId").equals(old.get("clusterId")))continue;
    List<Map<String,Object>> actual=maps(rows("id=?",String.valueOf(AccountCacheRecovery.id(old))));
    if(actual.size()!=1||!actual.get(0).equals(AccountCacheRecovery.loggedOut(old)))
     throw new UserError(R.string.error_other_cached_accounts_still_need_restoration_snapshot);
   }
   return true;
  }
  if(active.optBoolean("loggedIn"))throw new UserError(R.string.error_an_account_is_still_active_preserve_the);
  new AccountCacheRecovery.Store() {
   public List<Map<String,Object>> read()throws Exception {return maps(rows(null));}
   public void delete(Map<String,Object> expected)throws Exception {
    List<String> args=new ArrayList<>();String where=selection(expected,args);
    if(bridge.context.getContentResolver().delete(NativeBridge.ACCOUNTS,where,args.toArray(new String[0]))!=1)
     throw new UserError(R.string.error_custom_account_changed_during_removal_snapshot_preserved);
   }
   public void update(Map<String,Object> expected,Map<String,Object> restored)throws Exception {
    List<String> args=new ArrayList<>();String where=selection(expected,args);
    if(bridge.context.getContentResolver().update(NativeBridge.ACCOUNTS,values(json(restored)),where,args.toArray(new String[0]))!=1)
     throw new UserError(R.string.error_account_changed_during_restoration_snapshot_preserved);
   }
   public void insert(Map<String,Object> restored)throws Exception {
    if(rows("id=?",String.valueOf(AccountCacheRecovery.id(restored))).length()!=0)
     throw new UserError(R.string.error_original_account_row_id_is_occupied_snapshot);
    android.net.Uri inserted=bridge.context.getContentResolver().insert(NativeBridge.ACCOUNTS,values(json(restored)));
    if(inserted==null||android.content.ContentUris.parseId(inserted)!=AccountCacheRecovery.id(restored))
     throw new UserError(R.string.error_account_cache_restoration_failed_snapshot_preserved);
   }
   void restoreAll()throws Exception {AccountCacheRecovery.restore(maps(saved),this,uid,custom);}
  }.restoreAll();
  if(!previous.optBoolean("loggedIn"))return !bridge.account().optBoolean("loggedIn");
  // Type 21 selects the cached master implicitly; never guess among candidates.
  JSONArray masters=rows("parent IS NULL OR parent=''");
  if(!"Go103_2Lumi".equals(Build.MODEL)||masters.length()!=1||
     !previous.getString("uid").equals(masters.getJSONObject(0).optString("uid")))return false;
  JSONObject master=masters.getJSONObject(0);
  if(rows("uid=? OR accountId=?",master.getString("uid"),String.valueOf(master.get("accountId"))).length()!=1)return false;
  bridge.login(null,null,true);
  active=bridge.account();
  return active.optBoolean("loggedIn")&&previous.getString("uid").equals(active.optString("uid"))&&previous.getString("clusterId").equals(active.optString("clusterId"));
 }
}
