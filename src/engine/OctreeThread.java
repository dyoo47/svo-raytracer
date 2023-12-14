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
    //System.out.println("Generating octree with origin " + parentPos[0] + ", " + parentPos[1] + ", " + parentPos[2]);
    // System.out.println("Started octree thread");

    //create dummy head node
    octree.createDummyHead();
    octree.constructOctree(512, 0,9, parentPos, 0, voxelBuffer);
    // System.out.println("Octree construction complete.");
  }
  
}
