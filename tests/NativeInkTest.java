package dev.aragonite.powersync;

import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

public final class NativeInkTest {
 interface Checked {void run() throws Exception;}
 static int checks;
 static void rejects(String label,Checked test) throws Exception {
  try {test.run();throw new AssertionError("Accepted "+label);}catch(IOException expected){checks++;}
 }
 public static void main(String[] args) throws Exception {
  for(String path:args) {
   byte[] shape=null,points=null;String page=null,revision=null;
   try(ZipFile zip=new ZipFile(path)) {
    Enumeration<? extends ZipEntry> entries=zip.entries();
    while(entries.hasMoreElements()) {
     ZipEntry entry=entries.nextElement();String name=entry.getName();
     try(InputStream in=zip.getInputStream(entry)) {
      if(name.startsWith("document/")) shape=NativeInk.read(in);
      else {points=NativeInk.read(in);String[] parts=name.substring(name.lastIndexOf('/')+1).split("#");page=parts[0];revision=parts[1];}
     }
    }
   }
   final byte[] s=shape,p=points;final String pg=page,rev=revision;
   NativeInk.validate(s,p,pg,rev);checks++;
   String identity=NativeInk.semanticHash(s,p);
   byte[] proto;
   try(ZipInputStream source=new ZipInputStream(new ByteArrayInputStream(s))) {source.getNextEntry();proto=NativeInk.read(source);}
   String replacedRevision="12345678-1234-1234-1234-123456789abc";
   byte[] revisedProto=new String(proto,StandardCharsets.ISO_8859_1).replace(rev,replacedRevision).getBytes(StandardCharsets.ISO_8859_1);
   ByteArrayOutputStream resealed=new ByteArrayOutputStream();
   try(ZipOutputStream output=new ZipOutputStream(resealed)) {ZipEntry entry=new ZipEntry("different-container-name");entry.setTime(1234567890000L);output.putNextEntry(entry);output.write(revisedProto);output.closeEntry();}
   byte[] revisedPoints=p.clone();System.arraycopy(replacedRevision.getBytes(StandardCharsets.US_ASCII),0,revisedPoints,40,36);
   if(!identity.equals(NativeInk.semanticHash(resealed.toByteArray(),revisedPoints))) throw new AssertionError("Container churn changed ink identity");checks++;
   byte[] changed=p.clone();ByteBuffer.wrap(changed).putFloat(80,123.0f);
   if(identity.equals(NativeInk.semanticHash(s,changed))) throw new AssertionError("Point edit did not change ink identity");checks++;
   rejects("wrong page",()->NativeInk.validate(s,p,"bad",rev));
   rejects("wrong point revision",()->NativeInk.validate(s,p,pg,"bad"));
   rejects("truncated point file",()->NativeInk.validate(s,Arrays.copyOf(p,p.length-1),pg,rev));
   byte[] nan=p.clone();ByteBuffer.wrap(nan).putFloat(80,Float.NaN);
   rejects("NaN coordinate",()->NativeInk.validate(s,nan,pg,rev));
   byte[] index=p.clone();ByteBuffer.wrap(index).putInt(index.length-4,Integer.MAX_VALUE);
   rejects("invalid xref",()->NativeInk.validate(s,index,pg,rev));
   rejects("missing shape zip",()->NativeInk.validate(new byte[0],p,pg,rev));
  }
  rejects("duplicate protobuf field",()->NativeInk.fields(new byte[]{8,1,8,2}));
  rejects("truncated length",()->NativeInk.fields(new byte[]{10,4,1}));
  rejects("field zero",()->NativeInk.fields(new byte[]{0,1}));
  rejects("overflowing varint",()->NativeInk.fields(new byte[]{8,(byte)255,(byte)255,(byte)255,(byte)255,(byte)255,(byte)255,(byte)255,(byte)255,(byte)255,2}));
  if(!NativeInk.fields(new byte[]{(byte)0xa0,6,1}).containsKey(100)) throw new AssertionError("Unknown fields not parsed");checks++;
  System.out.println("NativeInk: "+checks+" integrity checks passed");
 }
}
