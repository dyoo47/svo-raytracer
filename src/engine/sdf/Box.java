package src.engine.sdf;

import src.engine.Util;

public class Box extends SignedDistanceField {

  int width;
  int height;
  int depth;

  public Box(int[] origin, int width, int height, int depth) {
    this.origin = origin;
    this.width = width;
    this.height = height;
    this.depth = depth;
    this.min = new int[] {
        origin[0] - (int) Math.ceil(width / 2.0f) - 1,
        origin[1] - (int) Math.ceil(height / 2.0f) - 1,
        origin[2] - (int) Math.ceil(depth / 2.0f) - 1,
    };
    this.max = new int[] {
        origin[0] + (int) Math.ceil(width / 2.0f) + 1,
        origin[1] + (int) Math.ceil(height / 2.0f) + 1,
        origin[2] + (int) Math.ceil(depth / 2.0f) + 1,
    };
  }

  @Override
  public int distance(int[] pos) {
    int[] b = {
        width / 2,
        height / 2,
        depth / 2
    };
    int[] q = {
        Math.abs(pos[0] - origin[0]),
        Math.abs(pos[1] - origin[1]),
        Math.abs(pos[2] - origin[2])
    };
    q = Util.subtractVectors(q, b);
    q = Util.max(q, 0);
    int m = Math.min(Math.max(q[0], Math.max(q[1], q[2])), 0);
    return (int) Util.length(Util.add(q, m));
  }

  @Override
  public short normal(int[] pos, boolean faceOutwards) {
    int invert = 1;
    if (faceOutwards)
      invert = -1;
    int[] diff = Util.subtractVectors(origin, pos);
    int[] dims = { width, height, depth };
    double[] quot = Util.divide(diff, dims);
    double max = Math.max(Math.abs(quot[0]), Math.max(Math.abs(quot[1]), Math.abs(quot[2])));
    if (Math.abs(quot[0]) == max)
      diff[0] = (int) Math.copySign(1, quot[0]) * invert;
    else
      diff[0] = 0;
    if (Math.abs(quot[1]) == max)
      diff[1] = (int) Math.copySign(1, quot[1]) * invert;
    else
      diff[1] = 0;
    if (Math.abs(quot[2]) == max)
      diff[2] = (int) Math.copySign(1, quot[2]) * invert;
    else
      diff[2] = 0;

    return Util.packNormal(Util.normalize(diff));
  }

}
