import java.nio.ByteBuffer;

public class OctreeStreamer {

  ByteCache memoryCache;
  ByteBuffer requestBuffer;

  public OctreeStreamer(){
    memoryCache = new ByteCache(Constants.CACHE_SIZE_MB);
    requestBuffer = ByteBuffer.allocateDirect(Constants.REQUEST_BUFFER_SIZE_KB * 1000);
  }

  public void processRequestBuffer(){
    
  }
}
