package com.chaos.tablist.net;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
/** Minecraft 1.20.1 UTF-8 and variable-length integer wire format, with bounded allocations. */
final class WireBuffer {
 private final ByteBuf buffer;
 WireBuffer(ByteBuf buffer) { this.buffer = buffer; }
 String readUtf(int limit) {
  int bytes = readVarInt();
  if(bytes < 0 || bytes > limit * 3 || bytes > buffer.readableBytes()) throw new IllegalArgumentException("Invalid UTF-8 length");
  String value = buffer.readCharSequence(bytes, StandardCharsets.UTF_8).toString();
  if(value.length() > limit) throw new IllegalArgumentException("String too long");
  return value;
 }
 void writeUtf(String value, int limit) {
  if(value.length() > limit) throw new IllegalArgumentException("String too long");
  byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
  if(bytes.length > limit * 3) throw new IllegalArgumentException("UTF-8 too long");
  writeVarInt(bytes.length); buffer.writeBytes(bytes);
 }
 int readVarInt() { int value=0; for(int i=0;i<5;i++) { int b=buffer.readUnsignedByte(); value |= (b&127) << (i*7); if((b&128)==0) return value; } throw new IllegalArgumentException("VarInt too long"); }
 long readVarLong() { long value=0; for(int i=0;i<10;i++) { int b=buffer.readUnsignedByte(); value |= (long)(b&127) << (i*7); if((b&128)==0) return value; } throw new IllegalArgumentException("VarLong too long"); }
 void writeVarInt(int value) { do { int b=value&127; value >>>=7; buffer.writeByte(value==0?b:b|128); } while(value!=0); }
 void writeVarLong(long value) { do { int b=(int)value&127; value >>>=7; buffer.writeByte(value==0?b:b|128); } while(value!=0); }
 boolean readBoolean() { return buffer.readBoolean(); }
 void writeBoolean(boolean value) { buffer.writeBoolean(value); }
 byte[] readByteArray(int limit) {
  int size=readVarInt();
  if(size<0 || size>limit || size>buffer.readableBytes()) throw new IllegalArgumentException("Invalid byte array length");
  byte[] bytes=new byte[size]; buffer.readBytes(bytes); return bytes;
 }
 void writeByteArray(byte[] bytes) { writeVarInt(bytes.length); buffer.writeBytes(bytes); }
}
