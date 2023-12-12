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

  @Test
  public void eoTest(){
    EfficientOctree eo = new EfficientOctree(1000000, 2048, Constants.WORLD_OFFSET);
    //eo.constructDebugOctree();
    eo.writeBufferToFile("debug.svo");
    // eo.printBufferToFile("debug.txt");
  }

  @Test
  public void gpuGenTest(){
    TestApp.launch(new TestApp());
  }

  @Test
  public void shiftTest(){
    int x = 12;
    int y = 1021 << 10;
    int z = 198 << 20;
    int sum = x | y | z;
    System.out.println(x);
    System.out.println(y);
    System.out.println(z);
    System.out.println(sum);
  }
  
}
