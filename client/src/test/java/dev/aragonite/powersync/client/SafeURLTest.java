package dev.aragonite.powersync.client;
import org.junit.Test;import static org.junit.Assert.*;
public class SafeURLTest {
 @Test public void allowsCustomHttpsOrigin()throws Exception {assertEquals("https://host.example:8443",SafeURL.base(" https://host.example:8443/ "));SafeURL.sameOrigin("https://host.example","wss://host.example/boox-neocloud");}
 @Test public void rejectsInsecureCredentialsAndForeignEndpoints()throws Exception {for(String s:new String[]{"http://host.example","https://name:secret@host.example","https://host.example/path","https://host.example?token=x","https://host.example#fragment","https://host.example:65536","https://host.example:0"}){try{SafeURL.base(s);fail(s);}catch(java.io.IOException expected){}}try{SafeURL.sameOrigin("https://host.example","https://other.example/objects");fail();}catch(java.io.IOException expected){}}
}
