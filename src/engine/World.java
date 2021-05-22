import java.nio.ByteBuffer;

public class World {

  int maxLOD;
  int size;
  int[] origin;
  public ByteBuffer octreeBuffer;
  ByteBuffer chunkBuffer;
  public VoxelData voxelData;
  WorldGenThread[] threads;
  EfficientOctree eo;

  public World(int maxLOD, int size){
    System.out.println("Initializing world...");
    this.maxLOD = maxLOD;
    this.size = size;
    voxelData = new VoxelData(size, size, size);
    threads = new WorldGenThread[8];
    this.origin = new int[]{0, -512, 0};
    for(int i=0; i < 8; i++){
      threads[i] = new WorldGenThread("wg-" + i, voxelData, Constants.childOffsets[i], origin);
    }
    System.out.println("Initialization complete!");
  }

  public void generateVoxelData(){
    System.out.println("Starting worldgen threads...");
    for(WorldGenThread t : threads){
      t.start();
    }
    int i = 0;
    while(i < 8){
      i = 0;
      for(WorldGenThread t : threads){
        if(!t.thread.isAlive()) i++;
      }
    }
    System.out.println("Voxel data generation complete!");
  }

  public void generateOctreeData(){
    eo = new EfficientOctree(1000000, voxelData);
    eo.constructOctree(maxLOD);
    octreeBuffer = eo.getByteBuffer();
  }


}
