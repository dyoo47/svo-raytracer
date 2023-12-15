package src.engine;

import java.nio.ByteBuffer;

public class OctreeThread extends Thread{

  Octree octree;
  int[] parentPos;
  ByteBuffer voxelBuffer;

  public OctreeThread(int[] parentPos, ByteBuffer voxelBuffer){
    this.parentPos = parentPos;
    this.voxelBuffer = voxelBuffer;
    octree = new Octree(Constants.OCTREE_MEMORY_SIZE_KB / 8);
  }

  @Override
  public void run() {
    octree.createDummyHead();
    octree.constructInnerOctree(512, 0,9, parentPos, 0, voxelBuffer);
  }
  
}
