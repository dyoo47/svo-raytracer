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

  public static int[] subtractVectors(int[] vec0, int[] vec1) {
    int[] diff = {
        vec0[0] - vec1[0],
        vec0[1] - vec1[1],
        vec0[2] - vec1[2]
    };
    return diff;
  }

  public static short packNormal(double[] normal) {
    int normalX = (int) (normal[0] * 9) / 2 + 5;
    int normalY = (int) (normal[1] * 9) / 2 + 5;
    int normalZ = (int) (normal[2] * 9) / 2 + 5;
    short packed = (short) (normalX + normalY * 10 + normalZ * 100);
    return packed;
  }

  public static double[] normalize(int[] vec) {
    double xsq = Math.pow(vec[0], 2);
    double ysq = Math.pow(vec[1], 2);
    double zsq = Math.pow(vec[2], 2);
    double length = Math.sqrt(xsq + ysq + zsq);
    double[] normalized = {
        vec[0] / length,
        vec[1] / length,
        vec[2] / length
    };
    return normalized;
  }
}
