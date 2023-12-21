package src.engine;

import java.nio.ByteBuffer;

public class OctreeThread extends Thread {

  Octree octree;
  int[] parentPos;
  ByteBuffer voxelBuffer;
  int maxLOD;

  public OctreeThread(int[] parentPos, ByteBuffer voxelBuffer, int maxLOD) {
    this.maxLOD = maxLOD;
    this.parentPos = parentPos;
    this.voxelBuffer = voxelBuffer;
    octree = new Octree(Constants.SUB_OCTREE_MEMORY_SIZE_KB);
  }

  @Override
  public void run() {
    octree.createDummyHead();
    octree.constructInnerOctree(512, 0, maxLOD, parentPos, 0, voxelBuffer);
  }

}
