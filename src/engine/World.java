import java.nio.ByteBuffer;

public class World {

  int maxLOD;
  int size;
  int[] origin;
  public ByteBuffer octreeBuffer;
  ByteBuffer chunkBuffer;
  EfficientOctree eo;

  public World(int maxLOD, int chunkSize){
    origin = new int[]{0, -512, 0};
    System.out.println("Initializing world...");
    this.maxLOD = maxLOD;
    this.size = chunkSize;
    double startTime = System.currentTimeMillis();
    generateOctreeData();
    double endTime = System.currentTimeMillis();
    System.out.println("Octree data generated in " + (endTime-startTime)/1000 + "s.");
    System.out.println("Initialization complete!");
  }

  private void generateOctreeData(){
    //eo = new EfficientOctree(30000, size, origin);
    eo = new EfficientOctree(100000, size, origin);
    eo.constructOctree(maxLOD, 0);
    //eo.constructOmegaTree();
    octreeBuffer = eo.getByteBuffer();
  }


}
