package dev.aragonite.powersync.client;

import java.io.IOException;
import java.util.*;

/** Pure recovery policy. Values contain credentials: never log rows or exceptions with values. */
final class AccountCacheRecovery {
 static final long SERVER_ACCOUNT_ID=2000000001L;
 interface Store {
  List<Map<String,Object>> read() throws Exception;
  void delete(Map<String,Object> expected) throws Exception;
  void update(Map<String,Object> expected,Map<String,Object> restored) throws Exception;
  void insert(Map<String,Object> restored) throws Exception;
 }
 static long id(Map<String,Object> row) throws IOException {
  Object value=row.get("id");
  if(!(value instanceof Number)||((Number)value).longValue()<=0)throw new IOException("Invalid cached account row ID");
  return ((Number)value).longValue();
 }
 static boolean affected(Map<String,Object> row,String uid) {
  return uid.equals(row.get("uid")) || Objects.equals(row.get("accountId"),SERVER_ACCOUNT_ID);
 }
 static Map<String,Object> loggedOut(Map<String,Object> row) {
  Map<String,Object> result=new HashMap<>(row);result.put("loggedIn",0L);return result;
 }
 private static Map<Long,Map<String,Object>> index(List<Map<String,Object>> rows)throws IOException {
  if(rows.size()>32)throw new IOException("Unexpected native account cache size");
  Map<Long,Map<String,Object>> result=new LinkedHashMap<>();
  for(Map<String,Object> row:rows)if(result.put(id(row),row)!=null)throw new IOException("Duplicate cached account row ID");
  return result;
 }
 static List<Map<String,Object>> capture(List<Map<String,Object>> rows,String uid)throws IOException {
  index(rows);
  List<Map<String,Object>> saved=new ArrayList<>();
  for(Map<String,Object> row:rows)if(affected(row,uid))saved.add(new HashMap<>(row));
  return saved;
 }
 // Native cluster switching logs out an original row before any custom login.
 // No credential/identity changes are accepted as an ordinary logout side effect.
 private static boolean onlyLogoutChanged(Map<String,Object> now,Map<String,Object> old) {
  Map<String,Object> a=new HashMap<>(now),b=new HashMap<>(old);
  for(String key:List.of("loggedIn","logOutAt","logoutReason","updatedAt")){a.remove(key);b.remove(key);}
  if(old.get("createdAt")==null&&(now.get("createdAt")==null||now.get("createdAt") instanceof Number)){a.remove("createdAt");b.remove("createdAt");}
  return a.equals(b);
 }
 static void verifyBeforeLogin(List<Map<String,Object>> saved,List<Map<String,Object>> current,String uid)throws IOException {
  Map<Long,Map<String,Object>> old=index(saved),now=index(capture(current,uid));
  if(!old.keySet().equals(now.keySet()))throw new IOException("Account cache changed before login; snapshot preserved");
  for(Long id:old.keySet())if(!onlyLogoutChanged(now.get(id),old.get(id)))
   throw new IOException("Cached credentials changed before login; snapshot preserved");
 }
 static void restore(List<Map<String,Object>> saved,Store store,String uid,String custom)throws Exception {
  Map<Long,Map<String,Object>> originals=index(saved);
  for(Map<String,Object> row:saved)if(!affected(row,uid))throw new IOException("Saved account outside restoration scope");
  List<Map<String,Object>> initial=store.read();index(initial);
  Set<Object> savedUIDs=new HashSet<>();for(Map<String,Object> row:saved)savedUIDs.add(row.get("uid"));
  List<Map<String,Object>> owned=new ArrayList<>();
  // Validate the entire restoration before deleting or replacing anything.
  for(Map<String,Object> row:initial) {
   if(uid.equals(row.get("uid"))&&custom.equals(row.get("clusterId"))) {owned.add(row);continue;}
   Map<String,Object> old=originals.get(id(row));
   if(old!=null) {
    if(!onlyLogoutChanged(row,old))throw new IOException("Original account row changed outside PowerSync; snapshot preserved");
   } else if(affected(row,uid)||savedUIDs.contains(row.get("uid"))) {
    throw new IOException("Account cache changed outside PowerSync; snapshot preserved");
   }
  }
  if(owned.size()>1)throw new IOException("Ambiguous custom account cache; snapshot preserved");
  List<Map<String,Object>> expected=new ArrayList<>(initial);
  for(Map<String,Object> row:owned) {
   unchanged(store,expected);store.delete(row);expected.remove(row);
  }
  for(Map<String,Object> old:saved) {
   unchanged(store,expected);
   Map<String,Object> restored=loggedOut(old);
   Map<String,Object> now=index(expected).get(id(old));
   if(now==null){store.insert(restored);expected.add(restored);}
   else if(!now.equals(restored)){store.update(now,restored);expected.remove(now);expected.add(restored);}
  }
  unchanged(store,expected); // Includes untouched unrelated accounts, not only restored rows.
 }
 private static void unchanged(Store store,List<Map<String,Object>> expected)throws Exception {
  if(!index(store.read()).equals(index(expected)))throw new IOException("Account cache changed during restoration; snapshot preserved");
 }
}
