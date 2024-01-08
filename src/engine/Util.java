package src.engine;

public class Util {

  public static int[] toVoxelSpace(float[] worldSpacePos) {
    int[] voxelSpacePos = {
        (int) ((worldSpacePos[0] - 1) * Constants.WORLD_SIZE),
        (int) ((worldSpacePos[1] - 1) * Constants.WORLD_SIZE),
        (int) ((worldSpacePos[2] - 1) * Constants.WORLD_SIZE)
    };
    return voxelSpacePos;
  }

  public static int getIntDistance(int[] pos0, int[] pos1) {
    double xsq = Math.pow(pos0[0] - pos1[0], 2);
    double ysq = Math.pow(pos0[1] - pos1[1], 2);
    double zsq = Math.pow(pos0[2] - pos1[2], 2);
    double dist = Math.sqrt(xsq + ysq + zsq);
    return (int) dist;
  }
}
