import java.nio.ByteBuffer;

public class ByteCache {
  int start;
  ByteBuffer buffer;
  int cacheSize;

  public ByteCache(int cacheSize){
    this.cacheSize = cacheSize;
    buffer = ByteBuffer.allocateDirect(cacheSize);
    start = cacheSize - 1;
  }

  public void appendByte(byte data){
    buffer.put(start, data);
    start = (start - 1) % cacheSize;
  }
}
