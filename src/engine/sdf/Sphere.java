package src.engine.sdf;

import src.engine.Util;

public class Sphere extends SignedDistanceField {

  int radius;

  public Sphere(int[] origin, int radius) {
    this.origin = origin;
    this.radius = radius;
    this.min = new int[] {
        origin[0] - radius - 1,
        origin[1] - radius - 1,
        origin[2] - radius - 1
    };
    this.max = new int[] {
        origin[0] + radius + 1,
        origin[1] + radius + 1,
        origin[2] + radius + 1
    };
  }

  @Override
  public int distance(int[] pos) {
    return Util.getIntDistance(pos, origin) - radius;
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
