package dev.aragonite.powersync.client;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.json.JSONObject;
public class AudioCacheRepairTest {
 @Rule public TemporaryFolder dir=new TemporaryFolder();
 final byte[] original="original synthetic audio".getBytes(java.nio.charset.StandardCharsets.UTF_8);
 final byte[] damaged="damaged! synthetic audio".getBytes(java.nio.charset.StandardCharsets.UTF_8);
 JSONObject packet()throws Exception{return new JSONObject().put("operationId",UUID.randomUUID().toString()).put("nativeUid","owner").put("documentId","test-note").put("fileName","recording.mp3").put("sha256",AudioCacheRepair.hash(original)).put("damagedSHA256",AudioCacheRepair.hash(damaged)).put("bytes",original.length).put("content",Base64.getEncoder().encodeToString(original));}
 Path target(Path root)throws Exception {Path p=root.resolve(".ksync/document/test-note/resource/data/recording.mp3");Files.createDirectories(p.getParent());return p;}
 @Test public void repairsOnlyExactDefectAndRetainsBackup()throws Exception {
  Path storage=dir.newFolder("storage").toPath(),backup=dir.newFolder("backup").toPath(),p=target(storage);Files.write(p,damaged);
  AudioCacheRepair repair=new AudioCacheRepair(packet(),"owner");assertEquals("repairable",repair.inspect(storage));repair.apply(storage,backup);assertArrayEquals(original,Files.readAllBytes(p));assertEquals("healthy",repair.inspect(storage));
  try(var files=Files.list(backup)){assertArrayEquals(damaged,Files.readAllBytes(files.findFirst().orElseThrow()));}
  assertThrows(IOException.class,()->repair.apply(storage,backup));
 }
 @Test public void preservesUnknownAndChangedFiles()throws Exception {
  Path storage=dir.newFolder("storage").toPath(),backup=dir.newFolder("backup").toPath(),p=target(storage);AudioCacheRepair repair=new AudioCacheRepair(packet(),"owner");assertEquals("missing",repair.inspect(storage));
  Files.write(p,damaged);assertEquals("repairable",repair.inspect(storage));byte[] edited="user edit".getBytes();Files.write(p,edited);
  assertThrows(IOException.class,()->repair.apply(storage,backup));assertArrayEquals(edited,Files.readAllBytes(p));
 }
 @Test public void rejectsForeignMalformedAndLinkedTargets()throws Exception {
  assertThrows(IOException.class,()->new AudioCacheRepair(packet(),"other"));
  for(String key:new String[]{"documentId","fileName","sha256"}){JSONObject p=packet().put(key,"../escape");assertThrows(IOException.class,()->new AudioCacheRepair(p,"owner"));}
  assertThrows(IOException.class,()->new AudioCacheRepair(packet().put("fileName","recording.pcm"),"owner"));
  assertThrows(IOException.class,()->new AudioCacheRepair(packet().put("content",Base64.getEncoder().encodeToString(damaged)),"owner"));
  Path storage=dir.newFolder("storage").toPath(),p=target(storage),outside=dir.newFile("outside").toPath();Files.write(outside,damaged);Files.createSymbolicLink(p,outside);
  AudioCacheRepair repair=new AudioCacheRepair(packet(),"owner");assertThrows(IOException.class,()->repair.inspect(storage));assertArrayEquals(damaged,Files.readAllBytes(outside));
 }
}
