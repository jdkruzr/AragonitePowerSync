package dev.aragonite.powersync;

import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Strict validator for the single-stroke lab payload, not a general importer. */
final class NativeInk {
 static String semanticHash(byte[] shapeZip,byte[] points) throws IOException {
  byte[] list;
  try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(shapeZip))) {
   if(zip.getNextEntry()==null) throw new IOException("Missing shape envelope");list=read(zip);
  }
  Map<Integer,Object> envelope=fields(list);
  if(!(envelope.get(1) instanceof byte[])) throw new IOException("Missing shape");
  byte[] shapeBody=(byte[])envelope.get(1);
  Map<Integer,Object> shape=fields(shapeBody);
  Map<Integer,Integer> wireTypes=new HashMap<>();ByteBuffer raw=ByteBuffer.wrap(shapeBody);
  while(raw.hasRemaining()) {
   long key=varint(raw);int field=(int)(key>>>3),wire=(int)(key&7);wireTypes.put(field,wire);
   if(wire==0) varint(raw);
   else {int length=(int)(wire==2?varint(raw):wire==1?8:4);raw.position(raw.position()+length);}
  }
  String page=padded(points,4),revision=string(shape.get(16));
  validate(shapeZip,points,page,revision);
  ByteBuffer b=ByteBuffer.wrap(points).order(ByteOrder.BIG_ENDIAN);
  int index=b.getInt(points.length-4),offset=b.getInt(index+36),length=b.getInt(index+40);
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();
  try(DataOutputStream canonical=new DataOutputStream(bytes)) {
   for(Map.Entry<Integer,Object> entry:new TreeMap<>(shape).entrySet()) {
    // Container revisions are regenerated on native export. Their referent
    // point records are included below, so ignoring the IDs loses no ink data.
    if(entry.getKey()==16 || entry.getKey()==18) continue;
    canonical.writeInt(entry.getKey());
    canonical.writeByte(wireTypes.get(entry.getKey()));
    if(entry.getValue() instanceof Long) canonical.writeLong((Long)entry.getValue());
    else {byte[] value=(byte[])entry.getValue();canonical.writeInt(value.length);canonical.write(value);}
   }
   canonical.writeInt(length);canonical.write(points,offset,length);
  }
  return digest(bytes.toByteArray());
 }
 static String digest(byte[] data) {
  try {
   byte[] bytes=java.security.MessageDigest.getInstance("SHA-256").digest(data);StringBuilder out=new StringBuilder();
   for(byte b:bytes) out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();
  } catch(java.security.NoSuchAlgorithmException e) {throw new AssertionError(e);}
 }
 static byte[] read(InputStream in) throws IOException {
  ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
  while((n=in.read(buffer))!=-1) {if(out.size()+n>1048576) throw new IOException("Ink entry exceeds limit");out.write(buffer,0,n);}
  return out.toByteArray();
 }
 static void validate(byte[] shapeZip,byte[] points,String page,String pointRevision) throws IOException {
  byte[] list;
  try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(shapeZip))) {
   ZipEntry first=zip.getNextEntry();
   if(first==null || first.isDirectory()) throw new IOException("Missing shape PB");
   list=read(zip);
   if(zip.getNextEntry()!=null) throw new IOException("Expected one shape PB");
  }
  Map<Integer,Object> envelope=fields(list);
  if(envelope.size()!=1 || !(envelope.get(1) instanceof byte[])) throw new IOException("Expected single shape envelope");
  Map<Integer,Object> shape=fields((byte[])envelope.get(1));
  String shapeId=string(shape.get(1));
  if(!shapeId.matches("[0-9a-fA-F-]{32,36}") || !pointRevision.equals(string(shape.get(16)))) throw new IOException("Shape point reference mismatch");
  if(points.length<128 || points.length>1048576) throw new IOException("Invalid point file size");
  ByteBuffer b=ByteBuffer.wrap(points).order(ByteOrder.BIG_ENDIAN);
  if(b.getShort(2)!=1 || !page.equals(padded(points,4)) || !pointRevision.equals(padded(points,40))) throw new IOException("Point header mismatch");
  int xref=b.getInt(points.length-4);
  if(xref<80 || xref!=points.length-48 || !shapeId.equals(padded(points,xref))) throw new IOException("Invalid point index");
  int offset=b.getInt(xref+36),length=b.getInt(xref+40);
  if(offset!=76 || length!=xref-76 || length<20 || (length-4)%16!=0) throw new IOException("Invalid point span");
  for(int p=80;p<xref;p+=16) {
   if(!Float.isFinite(b.getFloat(p)) || !Float.isFinite(b.getFloat(p+4))) throw new IOException("Non-finite stroke coordinate");
  }
 }
 private static String padded(byte[] data,int offset) throws IOException {
  if(offset<0 || offset>data.length-36) throw new IOException("Point string outside bounds");
  return new String(data,offset,36,StandardCharsets.US_ASCII).trim();
 }
 private static String string(Object value) throws IOException {
  if(!(value instanceof byte[])) throw new IOException("Missing string field");
  return new String((byte[])value,StandardCharsets.UTF_8);
 }
 private static long varint(ByteBuffer b) throws IOException {
  long value=0;
  for(int shift=0;shift<64;shift+=7) {
   if(!b.hasRemaining()) throw new IOException("Truncated protobuf varint");
   int next=b.get()&255;
   if(shift==63 && (next&254)!=0) throw new IOException("Overflowing protobuf varint");
   value|=(long)(next&127)<<shift;
   if(next<128) return value;
  }
  throw new IOException("Oversized protobuf varint");
 }
 static Map<Integer,Object> fields(byte[] data) throws IOException {
  ByteBuffer b=ByteBuffer.wrap(data);Map<Integer,Object> out=new HashMap<>();
  while(b.hasRemaining()) {
   long key=varint(b);int field=(int)(key>>>3),wire=(int)(key&7);
   if(field<=0 || key>>>3>536870911 || out.containsKey(field)) throw new IOException("Invalid or duplicate protobuf field");
   Object value;
   if(wire==0) value=varint(b);
   else {
    long length=wire==2?varint(b):wire==1?8:wire==5?4:-1;
    if(length<0 || length>b.remaining()) throw new IOException("Invalid protobuf field span");
    byte[] bytes=new byte[(int)length];b.get(bytes);value=bytes;
   }
   out.put(field,value);
  }
  return out;
 }
}
