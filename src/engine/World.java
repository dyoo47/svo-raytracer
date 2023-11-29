import java.nio.ByteBuffer;

public class World {

  int maxLOD;
  int size;
  int[] origin;
  ByteBuffer chunkBuffer;
  EfficientOctree eo;

  public World(int maxLOD, int chunkSize){
    init(maxLOD, chunkSize);
    generateOctreeData();
  }

public World(int maxLOD, int chunkSize, String fileName){
    init(maxLOD, chunkSize);
    eo = new EfficientOctree(10000000, size, origin);
    eo.readBufferFromFile(fileName);
  }

  private void init(int maxLOD, int chunkSize){
    origin = Constants.WORLD_OFFSET;
    System.out.println("Initializing world...");
    this.maxLOD = maxLOD;
    this.size = chunkSize;
  }

  private void generateOctreeData(){
    eo = new EfficientOctree(1000000, size, origin);
    OctreeThread ot = new OctreeThread("ot", eo, maxLOD);
    System.out.println("started octree thread");
    ot.start();
  }


}
