import java.nio.ByteBuffer;

public class World {

  int maxLOD;
  ByteBuffer octreeBuffer;
  ByteBuffer chunkBuffer;

  public World(int maxLOD){
    this.maxLOD = maxLOD;
  }
}
