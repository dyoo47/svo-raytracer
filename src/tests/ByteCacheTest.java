import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;

import org.junit.Test;
import org.lwjgl.BufferUtils;


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

  @Test
  public void bufferTest(){
    ByteBuffer parent = BufferUtils.createByteBuffer(10);
    ByteBuffer child = BufferUtils.createByteBuffer(4);
    ByteBuffer child1 = BufferUtils.createByteBuffer(4);
    child.put(0, (byte) 1);
    child.put(1, (byte) 2);
    child.position(0);
    child.limit(2);
    child1.put(0, (byte) 3);
    child1.put(1, (byte) 4);
    parent.put(child);
    parent.put(child1);
    for(int i=0; i < 10; i++){
      System.out.println(parent.get(i));
    }
  }
  
}
