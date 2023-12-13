import java.nio.ByteBuffer;

public class OctreeThread extends Thread{

  EfficientOctree octree;
  int[] parentPos;
  ByteBuffer voxelBuffer;

  public OctreeThread(int[] parentPos, ByteBuffer voxelBuffer){
    this.parentPos = parentPos;
    this.voxelBuffer = voxelBuffer;
    octree = new EfficientOctree(Constants.OCTREE_MEMORY_SIZE_KB / 8, 512, parentPos);
  }

  @Override
  public void run() {
    // System.out.println("Started octree thread");
    octree.constructOctree(512, 0, 8, parentPos, 0, voxelBuffer);
    // System.out.println("Octree construction complete.");
  }
  
}
