package src.engine.sdf;

import src.engine.Util;

public class Sphere implements SignedDistanceField {

  int[] origin;
  int radius;

  public Sphere(int[] origin, int radius) {
    this.origin = origin;
    this.radius = radius;
  }

  @Override
  public int distance(int[] pos) {
    return Util.getIntDistance(pos, origin) - radius;
  }
}
