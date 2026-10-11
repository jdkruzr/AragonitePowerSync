package dev.aragonite.powersync.client;
import org.junit.Test;
import org.json.*;
import static org.junit.Assert.*;
public class NativeConfigTest {
 @Test public void freshDeviceDefaultsOff() throws Exception {
  JSONObject c=NativeConfig.parse("ONYX_SYSTEM_CONFIG_KEY", "");
  assertEquals("false",NativeConfig.flag(c.getJSONObject("configMap"),"key_enable_sync_note_data"));
  assertEquals("false",NativeConfig.flag(c.getJSONObject("configMap"),"key_enable_sync_reader_data"));
 }
 @Test public void preservesExistingSettings() throws Exception {
  JSONObject c=NativeConfig.parse("ONYX_SYSTEM_CONFIG_KEY","{\"other\":42,\"configMap\":{\"key_enable_sync_note_data\":\"true\",\"unrelated\":\"keep\"}}");
  assertEquals(42,c.getInt("other"));assertEquals("keep",c.getJSONObject("configMap").getString("unrelated"));
  assertEquals("true",NativeConfig.flag(c.getJSONObject("configMap"),"key_enable_sync_note_data"));
 }
 @Test public void missingMapIsDefault() throws Exception {assertEquals(0,NativeConfig.parse("ONYX_SYSTEM_CONFIG_KEY","{}").getJSONObject("configMap").length());}
 @Test public void malformedConfigurationIsRejected() {
  for(String raw:new String[]{"broken","null","{\"configMap\":null}","{\"configMap\":7}"})assertThrows(JSONException.class,()->NativeConfig.parse("ONYX_SYSTEM_CONFIG_KEY",raw));
  assertThrows(JSONException.class,()->NativeConfig.parse("ONYX_CLUSTER_SYSTEM_KEY",""));
 }
 @Test public void malformedFlagsAreRejected() throws Exception {
  for(Object v:new Object[]{true,7,"yes",JSONObject.NULL}) {JSONObject c=new JSONObject().put("flag",v);assertThrows(JSONException.class,()->NativeConfig.flag(c,"flag"));}
 }
}
