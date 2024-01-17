package src.engine.sdf;

public abstract class SignedDistanceField {

  public int[] origin;
  public int[] min;
  public int[] max;

  public int distance(int[] pos) {
    return 0;
  }

  /**
   * 
   * @param pos a point on or neighboring the signed distance field
   * @return the surface normal of the given point
   */
  public short normal(int[] pos, boolean faceOutwards) {
    return (short) 0;
  }
}
