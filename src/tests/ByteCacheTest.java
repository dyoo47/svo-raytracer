package src.tests;

import src.engine.*;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.junit.Test;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

public class ByteCacheTest {

  @Test
  public void eval() {
    ByteCache byteCache = new ByteCache(3);
    byteCache.appendByte((byte) 1);
    byteCache.appendByte((byte) 1);
    byteCache.appendByte((byte) 1);
    byteCache.appendByte((byte) 2);
    assertEquals((byte) 2, byteCache.getBuffer().get(2));
    assertEquals((byte) 2, byteCache.getFirst());
  }

  @Test
  public void eoTest() {
    Octree eo = new Octree(1000000);
    // eo.constructDebugOctree();
    eo.writeBufferToFile("debug.svo");
    // eo.printBufferToFile("debug.txt");
  }

  @Test
  public void gpuGenTest() {
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
  public void bufferTest() {
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
    for (int i = 0; i < 10; i++) {
      System.out.println(parent.get(i));
    }
  }

  @Test
  public void imageTest() {
    ByteBuffer heightmapBuffer = BufferUtils.createByteBuffer(1024 * 1024 * 2);
    ShortBuffer heightmapShortBuffer;

    try (MemoryStack stack = MemoryStack.stackPush()) {

      IntBuffer width = stack.mallocInt(1);
      IntBuffer height = stack.mallocInt(1);
      IntBuffer channels = stack.mallocInt(1);

      File heightmapFile = new File("./assets/heightmaps/nz.png");
      String filePath = heightmapFile.getAbsolutePath();
      heightmapShortBuffer = STBImage.stbi_load_16(filePath, width, height, channels, 1);
      System.out.println(width.get(0) + ", " + height.get(0) + ", " + channels.get(0));
      heightmapBuffer.asShortBuffer().put(heightmapShortBuffer);
      System.out.println(heightmapBuffer.getShort(200));
    }
  }

  @Test
  public void asShortBufferTest() {
    ByteBuffer matmapBuffer = BufferUtils.createByteBuffer(1024 * 1024);
    try (MemoryStack stack = MemoryStack.stackPush()) {
      IntBuffer width = stack.mallocInt(1);
      IntBuffer height = stack.mallocInt(1);
      IntBuffer channels = stack.mallocInt(1);

      File heightmapFile = new File("./assets/matmaps/materials.png");
      String filePath = heightmapFile.getAbsolutePath();
      matmapBuffer = STBImage.stbi_load(filePath, width, height, channels, 1);

      System.out.println(matmapBuffer.get(126 + 1024 * 443));
    }
  }

  @Test
  public void anothertest() {

    double startTime = System.currentTimeMillis();
    ByteBuffer pixels = BufferUtils.createByteBuffer(Constants.WINDOW_WIDTH * Constants.WINDOW_HEIGHT * 4);
    pixels.put(1092, (byte) 19);
    int value = 0;
    for (int i = 0; i < Constants.WINDOW_HEIGHT * Constants.WINDOW_WIDTH; i++) {
      value += pixels.getInt(i * 4);
    }
    double endTime = System.currentTimeMillis() - startTime;
    System.out.println("time: " + endTime + " ms, value: " + value);
  }

}
