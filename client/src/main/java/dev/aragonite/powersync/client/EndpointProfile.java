package dev.aragonite.powersync.client;
import org.json.*;import java.io.IOException;
final class EndpointProfile {
 // Fields that may stay on stock Onyx (US): they never carry the account token, so firmware
 // updates, device configuration and the shop keep working. Every other field must be the server.
 static final java.util.Map<String,String> STOCK=java.util.Map.of("onyxCloudDataHostBaseUrl","https://en-data.boox.com","onyxLogHostBaseUrl","https://en-lon.boox.com","shopBookUrl","https://push.boox.com");
 static JSONObject validate(String base,JSONObject p)throws Exception {
  if(p.getInt("version")!=1||p.toString().length()>65536)throw new IOException("Unsupported server profile");
  java.util.UUID.fromString(p.getString("libraryId"));java.util.UUID.fromString(p.getString("deviceId"));String uid=p.getString("nativeUid");if(!uid.matches("[a-zA-Z0-9_-]{1,80}"))throw new IOException("Invalid native library identity");
  JSONObject c=p.getJSONObject("cluster");String id=c.getString("clusterId");if(!id.matches("powersync_[a-z0-9]{8,32}")||!id.equals(c.getString("lang")))throw new IOException("Invalid custom cluster identity");
  JSONObject h=c.getJSONObject("clusterHost");for(String k:new String[]{"onyxHostBaseUrl","onyxCloudDataHostBaseUrl","onyxLogHostBaseUrl","onyxContentHostBaseUrl","onyxSend2BooxHostBaseUrl","shopBookUrl","onyxMaterialHostBaseUrl","onyxCouchbaseWSSHost","onyxMessageReplicator"}){String v=h.getString(k);if(!v.equals(STOCK.get(k)))SafeURL.sameOrigin(base,v);}
  JSONObject o=c.getJSONObject("ossConfig");for(String k:new String[]{"ossNoteEndPoint","ossDeprecatedEndPoint","ossAIEndPoint","ossLogEndpoint","ossTestEndpoint"})SafeURL.sameOrigin(base,o.getString(k));for(String k:new String[]{"ossNoteBucket","ossDeprecatedBucket","ossAIBucket","ossLogBucket","ossTestBucket"})if(!o.getString(k).matches("[a-z0-9-]{3,63}"))throw new IOException("Invalid native bucket");
  JSONObject a=p.getJSONObject("account");if(a.getString("account").length()>200||a.getString("code").length()>128||a.getString("code").length()<6||p.getString("companionToken").length()<32)throw new IOException("Invalid enrollment credentials");p.put("serverURL",base);return p;
 }
 static JSONObject marker(JSONObject p)throws JSONException {JSONObject c=p.getJSONObject("cluster");return new JSONObject().put("serverId",c.getString("clusterId")).put("lang",c.getString("lang")).put("name",c.getString("name")).put("region",c.getString("region")).put("host",c.getJSONObject("clusterHost")).put("ossConfig",c.getJSONObject("ossConfig"));}
 // The installed entry with the server's endpoint values; names each changed value in changes.
 static JSONObject updatedEntry(JSONObject entry,JSONObject next,JSONArray changes)throws JSONException {JSONObject updated=new JSONObject(entry.toString());for(String part:new String[]{"clusterHost","ossConfig","couchbase","messageCouchbase"}){JSONObject from=entry.optJSONObject(part),to=next.optJSONObject(part);if(to==null)continue;if(from==null)from=new JSONObject();java.util.Iterator<String> keys=to.keys();while(keys.hasNext()){String k=keys.next();if(!to.getString(k).equals(from.optString(k,null)))changes.put(part+"."+k);}updated.put(part,new JSONObject(to.toString()));}return updated;}
 // The marker with exactly this server's entry replaced; other servers are kept byte-for-byte in value.
 static byte[] replaceMarker(byte[] raw,JSONObject profile)throws Exception {String id=profile.getJSONObject("cluster").getString("clusterId");JSONArray markers=new JSONArray(new String(raw,java.nio.charset.StandardCharsets.UTF_8));int at=-1;for(int i=0;i<markers.length();i++)if(id.equals(markers.getJSONObject(i).optString("serverId"))){if(at>=0)throw new IOException("Marker lists this server twice");at=i;}if(at<0)throw new IOException("Marker no longer lists this server; inspect before updating");markers.put(at,marker(profile));return markers.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);}
}
