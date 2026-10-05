package dev.aragonite.powersync;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import org.json.*;

/** Inspect without extracting. Hash logical entries independently of ZIP timestamps. */
final class NativeArchive {
 static JSONObject inkIdentity(File file,String documentId) throws Exception {
  JSONArray shapes=new JSONArray();
  try(ZipFile zip=new ZipFile(file)) {
   Enumeration<? extends ZipEntry> entries=zip.entries();
   while(entries.hasMoreElements()) {
    ZipEntry entry=entries.nextElement();String name=entry.getName();
    if(!name.startsWith(documentId+"/shape/") || !name.endsWith(".zip")) continue;
    byte[] shapeBytes;
    try(InputStream in=zip.getInputStream(entry)) {shapeBytes=NativeInk.read(in);}
    java.util.Map<Integer,Object> shape;
    try(ZipInputStream nested=new ZipInputStream(new ByteArrayInputStream(shapeBytes))) {
     if(nested.getNextEntry()==null) throw new IOException("Missing shape envelope");
     Object envelope=NativeInk.fields(NativeInk.read(nested)).get(1);
     if(!(envelope instanceof byte[])) throw new IOException("Missing shape");
     shape=NativeInk.fields((byte[])envelope);
    }
    if(!Long.valueOf(3).equals(shape.get(12))) throw new IOException("Ink identity currently supports single oil-pen shapes only");
    String page=name.substring(name.lastIndexOf('/')+1).split("#")[0];
    String revision=new String((byte[])shape.get(16),java.nio.charset.StandardCharsets.UTF_8);
    ZipEntry point=zip.getEntry(documentId+"/point/"+page+"/"+page+"#"+revision+"#points");
    if(point==null) throw new IOException("Missing referenced point asset");
    try(InputStream in=zip.getInputStream(point)) {
     shapes.put(new JSONObject().put("pageId",page).put("shapeId",new String((byte[])shape.get(1),java.nio.charset.StandardCharsets.UTF_8))
      .put("sha256",NativeInk.semanticHash(shapeBytes,NativeInk.read(in))));
    }
   }
  }
  return new JSONObject().put("scope","single-oil-pen-shape-records").put("shapes",shapes).put("completeNotebookIdentity",false);
 }
 static JSONObject inspect(File file) throws Exception {
  if(file.length()>268435456L) throw new IOException("Archive exceeds lab size limit");
  TreeMap<String,JSONObject> entries=new TreeMap<>(); long total=0;
  try(ZipFile zip=new ZipFile(file)) {
   Enumeration<? extends ZipEntry> list=zip.entries();
   byte[] buffer=new byte[16384];
   while(list.hasMoreElements()) {
    ZipEntry entry=list.nextElement(); String name=entry.getName();
    if(name.startsWith("/") || name.contains("\\") || Arrays.asList(name.split("/",-1)).contains("..")) throw new IOException("Unsafe archive entry");
    if(entry.isDirectory()) continue;
    if(entries.size()>=10000 || entries.containsKey(name)) throw new IOException("Duplicate or excessive entries");
    MessageDigest hash=MessageDigest.getInstance("SHA-256"); CRC32 crc=new CRC32(); long size=0;
    try(InputStream in=zip.getInputStream(entry)) {
     int n; while((n=in.read(buffer))!=-1) {
      size+=n;total+=n;
      if(total>536870912L) throw new IOException("Expanded archive exceeds lab limit");
      hash.update(buffer,0,n);crc.update(buffer,0,n);
     }
    }
    if(size!=entry.getSize() || crc.getValue()!=entry.getCrc()) throw new IOException("Archive integrity mismatch");
    entries.put(name,new JSONObject().put("name",name).put("bytes",size).put("sha256",hex(hash.digest())));
   }
  }
  if(entries.isEmpty()) throw new IOException("Empty native archive");
  JSONArray manifest=new JSONArray(); MessageDigest logical=MessageDigest.getInstance("SHA-256");
  for(JSONObject entry:entries.values()) {
   manifest.put(entry);
   logical.update((entry.getString("name")+"\u0000"+entry.getLong("bytes")+"\u0000"+entry.getString("sha256")+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }
  return new JSONObject().put("entries",manifest).put("expandedBytes",total).put("logicalSha256",hex(logical.digest())).put("zipIntegrityVerified",true);
 }
 private static String hex(byte[] bytes) {StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
}
