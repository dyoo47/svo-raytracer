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
    WorldGenerator.launch(new WorldGenerator());
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
