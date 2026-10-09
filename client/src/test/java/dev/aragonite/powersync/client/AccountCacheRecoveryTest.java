package dev.aragonite.powersync.client;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.IOException;
import java.util.*;

public class AccountCacheRecoveryTest {
 private static Map<String,Object> row(long id,String uid,long account,String cluster) {
  Map<String,Object> r=new HashMap<>();r.put("id",id);r.put("uid",uid);r.put("accountId",account);r.put("clusterId",cluster);r.put("loggedIn",0L);r.put("token","fixture-"+id);r.put("logOutAt",12L);r.put("logoutReason",0L);r.put("parent",null);return r;
 }
 static class Memory implements AccountCacheRecovery.Store {
  List<Map<String,Object>> data=new ArrayList<>();int writes,stopAfter=-1;
  Memory(List<Map<String,Object>> rows){for(Map<String,Object> r:rows)data.add(new HashMap<>(r));}
  public List<Map<String,Object>> read(){List<Map<String,Object>> copy=new ArrayList<>();for(Map<String,Object> r:data)copy.add(new HashMap<>(r));return copy;}
  void wrote()throws IOException {if(++writes==stopAfter)throw new IOException("simulated process loss after mutation");}
  public void delete(Map<String,Object> old)throws Exception {assertTrue(data.remove(old));wrote();}
  public void update(Map<String,Object> old,Map<String,Object> restored)throws Exception {assertTrue(data.remove(old));data.add(new HashMap<>(restored));wrote();}
  public void insert(Map<String,Object> restored)throws Exception {for(Map<String,Object> r:data)assertNotEquals(r.get("id"),restored.get("id"));data.add(new HashMap<>(restored));wrote();}
 }
 private List<Map<String,Object>> originals(){return List.of(row(1,"owner",123,"en"),row(2,"old-lab",2000000001L,"old-server"));}
 @Test public void capturesBothDeletionPredicatesWithoutUnrelatedCredentials()throws Exception {
  List<Map<String,Object>> all=new ArrayList<>(originals());all.add(row(3,"other",999,"eu"));
  assertEquals(originals(),AccountCacheRecovery.capture(all,"owner"));
 }
 @Test public void restoresAllRowsAndLeavesOtherAccountsExact()throws Exception {
  Map<String,Object> unrelated=row(3,"other",999,"eu");
  Memory m=new Memory(List.of(row(4,"owner",2000000001L,"custom"),unrelated));
  AccountCacheRecovery.restore(originals(),m,"owner","custom");
  assertEquals(new HashSet<>(List.of(originals().get(0),originals().get(1),unrelated)),new HashSet<>(m.read()));
  int writes=m.writes;AccountCacheRecovery.restore(originals(),m,"owner","custom");assertEquals(writes,m.writes);
 }
 @Test public void recoveryResumesAfterEveryMutation()throws Exception {
  for(int interruption=1;interruption<=3;interruption++) {
   Memory m=new Memory(List.of(row(4,"owner",2000000001L,"custom")));m.stopAfter=interruption;
   try{AccountCacheRecovery.restore(originals(),m,"owner","custom");fail();}catch(IOException expected){}
   m.stopAfter=-1;AccountCacheRecovery.restore(originals(),m,"owner","custom");assertEquals(new HashSet<>(originals()),new HashSet<>(m.read()));
  }
 }
 @Test public void unrelatedOccupiedIDAndChangedCredentialsRejectBeforeAnyWrite()throws Exception {
  Map<String,Object> changed=new HashMap<>(originals().get(0));changed.put("token","new external credentials");
  for(Map<String,Object> collision:List.of(row(1,"foreign",998,"eu"),changed,row(9,"old-lab",321,"other-cluster"))) {
   Memory m=new Memory(List.of(row(4,"owner",2000000001L,"custom"),collision));List<Map<String,Object>> before=m.read();
   try{AccountCacheRecovery.restore(originals(),m,"owner","custom");fail();}catch(IOException expected){}
   assertEquals(0,m.writes);assertEquals(before,m.read());
  }
 }
 @Test public void reusedCustomRowIDCanBeRecovered()throws Exception {
  Memory m=new Memory(List.of(row(2,"owner",2000000001L,"custom")));
  AccountCacheRecovery.restore(originals(),m,"owner","custom");assertEquals(new HashSet<>(originals()),new HashSet<>(m.read()));
 }
 @Test public void sameOwnerAcrossClustersIsPreserved()throws Exception {
  List<Map<String,Object>> saved=List.of(row(1,"owner",123,"en"),row(2,"owner",124,"eu"));
  Memory m=new Memory(List.of(row(3,"owner",2000000001L,"custom")));
  AccountCacheRecovery.restore(saved,m,"owner","custom");assertEquals(new HashSet<>(saved),new HashSet<>(m.read()));
 }
 @Test public void logoutSideEffectsAcceptedButPreLoginTokenChangeRejected()throws Exception {
  List<Map<String,Object>> saved=originals();Memory m=new Memory(saved);
  m.data.get(0).put("logOutAt",100L);m.data.get(0).put("logoutReason",1L);m.data.get(0).put("updatedAt",100L);m.data.get(0).put("createdAt",100L);
  AccountCacheRecovery.verifyBeforeLogin(saved,m.read(),"owner");
  AccountCacheRecovery.restore(saved,m,"owner","custom");assertEquals(new HashSet<>(saved),new HashSet<>(m.read()));
  m.data.get(0).put("token","changed");
  try{AccountCacheRecovery.verifyBeforeLogin(saved,m.read(),"owner");fail();}catch(IOException expected){}
 }
 @Test public void preLoginNewCollisionRejected()throws Exception {
  Memory m=new Memory(originals());m.data.add(row(8,"unexpected",2000000001L,"foreign"));
  try{AccountCacheRecovery.verifyBeforeLogin(originals(),m.read(),"owner");fail();}catch(IOException expected){}
 }
 @Test public void emptySnapshotOnlyRemovesOwnedCustomAccount()throws Exception {
  Map<String,Object> unrelated=row(3,"other",999,"eu");Memory m=new Memory(List.of(row(4,"owner",2000000001L,"custom"),unrelated));
  AccountCacheRecovery.restore(List.of(),m,"owner","custom");assertEquals(List.of(unrelated),m.read());
 }
}
