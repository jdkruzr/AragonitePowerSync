package dev.aragonite.powersync.client;
import org.json.*;import java.io.IOException;
final class EndpointProfile {
 static JSONObject validate(String base,JSONObject p)throws Exception {
  if(p.getInt("version")!=1||p.toString().length()>65536)throw new IOException("Unsupported server profile");
  java.util.UUID.fromString(p.getString("libraryId"));java.util.UUID.fromString(p.getString("deviceId"));String uid=p.getString("nativeUid");if(!uid.matches("[a-zA-Z0-9_-]{1,80}"))throw new IOException("Invalid native library identity");
  JSONObject c=p.getJSONObject("cluster");String id=c.getString("clusterId");if(!id.matches("powersync_[a-z0-9]{8,32}")||!id.equals(c.getString("lang")))throw new IOException("Invalid custom cluster identity");
  JSONObject h=c.getJSONObject("clusterHost");for(String k:new String[]{"onyxHostBaseUrl","onyxCloudDataHostBaseUrl","onyxLogHostBaseUrl","onyxContentHostBaseUrl","onyxSend2BooxHostBaseUrl","shopBookUrl","onyxMaterialHostBaseUrl","onyxCouchbaseWSSHost","onyxMessageReplicator"})SafeURL.sameOrigin(base,h.getString(k));
  JSONObject o=c.getJSONObject("ossConfig");for(String k:new String[]{"ossNoteEndPoint","ossDeprecatedEndPoint","ossAIEndPoint","ossLogEndpoint","ossTestEndpoint"})SafeURL.sameOrigin(base,o.getString(k));for(String k:new String[]{"ossNoteBucket","ossDeprecatedBucket","ossAIBucket","ossLogBucket","ossTestBucket"})if(!o.getString(k).matches("[a-z0-9-]{3,63}"))throw new IOException("Invalid native bucket");
  JSONObject a=p.getJSONObject("account");if(a.getString("account").length()>200||a.getString("code").length()>128||a.getString("code").length()<6||p.getString("companionToken").length()<32)throw new IOException("Invalid enrollment credentials");p.put("serverURL",base);return p;
 }
 static JSONObject marker(JSONObject p)throws JSONException {JSONObject c=p.getJSONObject("cluster");return new JSONObject().put("serverId",c.getString("clusterId")).put("lang",c.getString("lang")).put("name",c.getString("name")).put("region",c.getString("region")).put("host",c.getJSONObject("clusterHost")).put("ossConfig",c.getJSONObject("ossConfig"));}
}
