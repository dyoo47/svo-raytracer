import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ByteCacheTest {
  
  @Test
  public void eval(){
    ByteCache byteCache = new ByteCache(3);
    byteCache.appendByte((byte)1);
    byteCache.appendByte((byte)1);
    byteCache.appendByte((byte)1);
    byteCache.appendByte((byte)2);
    assertEquals((byte)2, byteCache.getBuffer().get(2));
    assertEquals((byte)2, byteCache.getFirst());
  }
}
