package src.tests;

import src.engine.*;
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
    Octree eo = new Octree(1000000);
    //eo.constructDebugOctree();
    eo.writeBufferToFile("debug.svo");
    // eo.printBufferToFile("debug.txt");
  }

  @Test
  public void gpuGenTest(){
    short leafMask = 0;
    int n = 1;
    int x = 1;
    int y = 1;
    short result = (short) (leafMask | (0x0001 << (n << 1)));
    result |= (short) (0x0002 << (x << 1));
    short local = (short) ((result & (0x0003 << (y << 1))) >> (y << 1));
    System.out.println(local);
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
