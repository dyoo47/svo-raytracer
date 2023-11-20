import java.nio.ByteBuffer;

public class ByteCache {
  int start;
  ByteBuffer buffer;
  int cacheSize;

  public ByteCache(int cacheSize){
    this.cacheSize = cacheSize;
    buffer = ByteBuffer.allocateDirect(cacheSize);
    start = cacheSize;
  }

  public void appendByte(byte data){
    start = (start - 1) % cacheSize;
    if(start < 0) start += cacheSize;
    buffer.put(start, data);
  }

  public byte getFirst(){
    return buffer.get(start);
  }

  public ByteBuffer getBuffer(){
    return buffer;
  }
}
