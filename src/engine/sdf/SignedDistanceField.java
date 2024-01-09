package src.engine.sdf;

public interface SignedDistanceField {

  int distance(int[] pos);

  /**
   * 
   * @param pos a point on or neighboring the signed distance field
   * @return the surface normal of the given point
   */
  short normal(int[] pos, boolean faceInwards);
}
