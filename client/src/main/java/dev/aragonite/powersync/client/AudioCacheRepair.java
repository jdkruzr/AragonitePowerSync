package dev.aragonite.powersync.client;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.json.JSONObject;

/** Exact known-defect replacement only. No cache eviction or native library writes. */
final class AudioCacheRepair {
 static final int MAX_BYTES=16*1024*1024;
 final String operation,uid,document,name,sha,damagedSHA;
 final byte[] content;
 AudioCacheRepair(JSONObject packet,String expectedUid)throws Exception {
  operation=packet.getString("operationId");UUID.fromString(operation);
  uid=packet.getString("nativeUid");document=packet.getString("documentId");name=packet.getString("fileName");
  if(!uid.equals(expectedUid)||!document.matches("[A-Za-z0-9_-]{1,80}")||!name.matches("[A-Za-z0-9_-]{1,100}\\.mp3"))throw new UserError(R.string.error_audio_identity_is_not_supported);
  sha=packet.getString("sha256");damagedSHA=packet.getString("damagedSHA256");
  if(!sha.matches("[a-f0-9]{64}")||!damagedSHA.matches("[a-f0-9]{64}"))throw new UserError(R.string.error_invalid_audio_integrity_proof);
  long size=packet.getLong("bytes");String encoded=packet.getString("content");
  if(size<0||size>MAX_BYTES||encoded.length()>((MAX_BYTES+2)/3)*4)throw new UserError(R.string.error_recording_exceeds_the_recovery_limit);
  content=Base64.getDecoder().decode(encoded);
  if(content.length!=size||!hash(content).equals(sha))throw new UserError(R.string.error_audio_integrity_check_failed);
 }
 private Path target(Path storage)throws IOException {
  Path root=storage.toAbsolutePath().normalize();Path p=root.resolve(".ksync/document").resolve(document).resolve("resource/data").resolve(name);
  Path check=root;for(Path part:root.relativize(p)){check=check.resolve(part);if(Files.isSymbolicLink(check))throw new UserError(R.string.error_linked_audio_paths_are_not_supported);}
  if(!p.toFile().getCanonicalFile().toPath().equals(p))throw new UserError(R.string.error_audio_path_changed);
  return p;
 }
 String inspect(Path storage)throws Exception {
  Path p=target(storage);
  if(!Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS))return "missing";
  if(Files.size(p)!=content.length)return "different";
  String current=hash(Files.readAllBytes(p));
  if(current.equals(sha))return "healthy";
  return current.equals(damagedSHA)?"repairable":"different";
 }
 void apply(Path storage,Path privateBackup)throws Exception {
  if(!"repairable".equals(inspect(storage)))throw new UserError(R.string.error_recording_changed_or_does_not_match_the);
  Path p=target(storage);byte[] before=Files.readAllBytes(p);
  if(!hash(before).equals(damagedSHA))throw new UserError(R.string.error_recording_changed_before_backup);
  Files.createDirectories(privateBackup);
  Path backup=privateBackup.resolve(operation+"-"+damagedSHA+".mp3");
  if(Files.exists(backup)){if(!hash(Files.readAllBytes(backup)).equals(damagedSHA))throw new UserError(R.string.error_previous_audio_backup_differs);}
  else{writeSynced(backup,before);}
  // Stage outside the native resource directory so ksync cannot upload a temp file.
  Path staging=storage.resolve(".powersync-audio-recovery");
  if(Files.isSymbolicLink(staging))throw new UserError(R.string.error_linked_staging_directory_is_not_supported);
  Files.createDirectories(staging);Path temp=Files.createTempFile(staging,"recording-",".tmp");
  try{
   writeSynced(temp,content);
   if(!"repairable".equals(inspect(storage)))throw new UserError(R.string.error_recording_changed_during_recovery_backup_kept_nothing);
   Files.move(temp,p,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
   if(!"healthy".equals(inspect(storage)))throw new UserError(R.string.error_recording_changed_after_replacement_backup_retained_for);
  }finally{Files.deleteIfExists(temp);}
 }
 private static void writeSynced(Path path,byte[] data)throws IOException {try(FileOutputStream out=new FileOutputStream(path.toFile())){out.write(data);out.getFD().sync();}}
 static String hash(byte[] bytes)throws Exception {StringBuilder b=new StringBuilder();for(byte v:MessageDigest.getInstance("SHA-256").digest(bytes))b.append(String.format(Locale.ROOT,"%02x",v&255));return b.toString();}
}
