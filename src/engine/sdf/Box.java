package src.engine.sdf;

import src.engine.Util;

public class Box implements SignedDistanceField {

  int[] origin;
  int width;
  int height;
  int depth;

  public Box(int[] origin, int width, int height, int depth) {
    this.origin = origin;
    this.width = width;
    this.height = height;
    this.depth = depth;
  }

  @Override
  public int distance(int[] pos) {
    int[] b = {
        width,
        height,
        depth
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
    int[] diff;
    if (faceOutwards) {
      diff = Util.subtractVectors(pos, origin);
    } else {
      diff = Util.subtractVectors(origin, pos);
    }
    return Util.packNormal(Util.normalize(diff));
  }

}
