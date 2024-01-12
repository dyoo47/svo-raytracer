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
    return (int) Math.round(dist);
  }

  public static double length(int[] vec0) {
    return Math.sqrt(
        Math.pow(vec0[0], 2) +
            Math.pow(vec0[1], 2) +
            Math.pow(vec0[2], 2));
  }

  public static float[] invert(float[] vec0) {
    float[] invert = {
        -vec0[0],
        -vec0[1],
        -vec0[2]
    };
    return invert;
  }

  public static int[] add(int[] vec0, int scalar0) {
    int[] sum = {
        vec0[0] + scalar0,
        vec0[1] + scalar0,
        vec0[2] + scalar0,
    };
    return sum;
  }

  public static float[] add(float[] vec0, float[] vec1) {
    float[] sum = {
        vec0[0] + vec1[0],
        vec0[1] + vec1[1],
        vec0[2] + vec1[2],
    };
    return sum;
  }

  public static float[] mix(float[] vec0, float[] vec1) {
    return mix(vec0, vec1, 0.5f);
  }

  public static float[] mix(float[] vec0, float[] vec1, float a) {
    float[] lerp = {
        vec0[0] * (1 - a) + vec1[0] * a,
        vec0[1] * (1 - a) + vec1[1] * a,
        vec0[2] * (1 - a) + vec1[2] * a
    };
    return lerp;
  }

  public static int[] subtractVectors(int[] vec0, int[] vec1) {
    int[] diff = {
        vec0[0] - vec1[0],
        vec0[1] - vec1[1],
        vec0[2] - vec1[2]
    };
    return diff;
  }

  public static float[] mult(float[] vec0, float scalar0) {
    float[] product = {
        vec0[0] * scalar0,
        vec0[1] * scalar0,
        vec0[2] * scalar0
    };
    return product;
  }

  public static int[] max(int[] vec0, int[] vec1) {
    int[] max = {
        Math.max(vec0[0], vec1[0]),
        Math.max(vec0[1], vec1[1]),
        Math.max(vec0[2], vec1[2])
    };
    return max;
  }

  public static int[] max(int[] vec0, int scalar0) {
    int[] max = {
        Math.max(vec0[0], scalar0),
        Math.max(vec0[1], scalar0),
        Math.max(vec0[2], scalar0)
    };
    return max;
  }

  public static int[] min(int[] vec0, int[] vec1) {
    int[] min = {
        Math.min(vec0[0], vec1[0]),
        Math.min(vec0[1], vec1[1]),
        Math.min(vec0[2], vec1[2])
    };
    return min;
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
