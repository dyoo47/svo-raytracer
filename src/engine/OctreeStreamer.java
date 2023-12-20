package src.engine;

import java.nio.ByteBuffer;

public class OctreeStreamer {

  ByteCache memoryCache;
  ByteBuffer requestBuffer;

  public OctreeStreamer() {
    memoryCache = new ByteCache(Constants.CACHE_SIZE_MB);
    requestBuffer = ByteBuffer.allocateDirect(Constants.REQUEST_BUFFER_SIZE_KB * 1000);
    // requestBuffer = ByteBuffer.allocateDirect(10);
  }

  public void processRequestBuffer() {

  }

  public void printBuffer(int elements) {
    System.out.println("Printing request buffer:");
    System.out.println(requestBuffer.toString());
    for (int i = 0; i < elements; i++) {
      System.out.println(requestBuffer.get(i));
    }
  }
}
