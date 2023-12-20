package src.engine;

import java.nio.ByteBuffer;

public class ByteCache {
  private int start;
  private ByteBuffer buffer;
  private int cacheSize;

  public ByteCache(int cacheSizeMb) {
    this.cacheSize = cacheSizeMb;
    buffer = ByteBuffer.allocateDirect(cacheSizeMb * 1000000);
    start = cacheSizeMb;
  }

  public void appendByte(byte data) {
    start = (start - 1) % cacheSize;
    if (start < 0)
      start += cacheSize;
    buffer.put(start, data);
  }

  public byte getFirst() {
    return buffer.get(start);
  }

  public ByteBuffer getBuffer() {
    return buffer;
  }

  public void printBuffer() {
    System.out.println("Printing byte cache of size " + cacheSize + "MB");
    for (byte b : buffer.array()) {
      System.out.println(b);
    }
  }
}
