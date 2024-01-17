package src.engine;

import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

public class Camera {

  float[] pos;
  float speed;
  float scaleX = 0.9f;
  float scaleY = 1.6f;
  float[] l1 = { -scaleY, -scaleX, -1 };
  float[] l2 = { -scaleY, scaleX, -1 };
  float[] r1 = { scaleY, -scaleX, -1 };
  float[] r2 = { scaleY, scaleX, -1 };
  float[] rot = { 0, 0, 0 };
  float[] dir = { 0, 0, 1 };
  float[] right = { 1, 0, 0 };

  public Camera() {
    pos = new float[3];
    pos[0] = 0.0f;
    pos[1] = 0.0f;
    pos[2] = 0.0f;
    speed = 0.005f;
  }

  public int[] getRayPickLocation(float depth) {
    float[] worldLoc = Util.add(Util.mult(Util.invert(dir), depth), pos);
    return Util.toVoxelSpace(worldLoc);
  }

  public void setSpeed(float speed) {
    this.speed = speed;
  }

  public void setPos(float x, float y, float z) {
    pos[0] = x;
    pos[1] = y;
    pos[2] = z;
  }

  public void strafe(float forwardSpeed, float sideSpeed) {
    pos[0] += -dir[0] * speed * forwardSpeed + right[0] * speed * sideSpeed;
    pos[1] += -dir[1] * speed * forwardSpeed + right[1] * speed * sideSpeed;
    pos[2] += -dir[2] * speed * forwardSpeed + right[2] * speed * sideSpeed;
  }

  public float[] getPos() {
    return pos;
  }

  private float[] convertToCameraXAxis(float x, float y, float z) {
    float alpha = rot[1];

    float xp = (float) (x * Math.cos(alpha) + z * Math.sin(alpha));
    float yp = y;
    float zp = (float) (z * Math.cos(alpha) - x * Math.sin(alpha));

    float[] out = { xp, yp, zp };
    return out;
  }

  private FloatBuffer updateDirection(float x, float y, float z) {
    float[] frontf = { 0, 0, 1 };
    Vector3f front = new Vector3f(frontf);
    front.rotateX(x).rotateY(y).rotateZ(z);
    FloatBuffer out = BufferUtils.createFloatBuffer(3);
    front.get(out);
    return out;
  }

  public void rotate(float x, float y, float z) {

    if (rot[0] + x < Constants.CAMERA_LOWER_LIMIT || rot[0] + x > Constants.CAMERA_UPPER_LIMIT) {
      if (x > 0)
        rot[0] = Constants.CAMERA_UPPER_LIMIT;
      else
        rot[0] = Constants.CAMERA_LOWER_LIMIT;
      x = 0;
    } else {
      rot[0] += x;
    }
    rot[1] += y;
    rot[1] %= Constants.Math.PI * 2;
    rot[2] += z;
    rot[2] %= Constants.Math.PI * 2;
    Matrix4f matrix = new Matrix4f();
    right = convertToCameraXAxis(1, 0, 0);

    matrix.rotate(y, 0, 1, 0);
    matrix.rotate(x, right[0], right[1], right[2]);

    FloatBuffer dirf = updateDirection(rot[0], rot[1], rot[2]);
    dir[0] = dirf.get(0);
    dir[1] = dirf.get(1);
    dir[2] = dirf.get(2);

    FloatBuffer fBuffer = BufferUtils.createFloatBuffer(16);
    matrix.get(fBuffer);

    float a = fBuffer.get(0);
    float b = fBuffer.get(4);
    float c = fBuffer.get(8);
    float d = fBuffer.get(1);
    float e = fBuffer.get(5);
    float f = fBuffer.get(9);
    float g = fBuffer.get(2);
    float h = fBuffer.get(6);
    float i = fBuffer.get(10);

    float[] nl1 = new float[3];
    float[] nl2 = new float[3];
    float[] nr1 = new float[3];
    float[] nr2 = new float[3];

    nl1[0] = a * l1[0] + b * l1[1] + c * l1[2];
    nl1[1] = d * l1[0] + e * l1[1] + f * l1[2];
    nl1[2] = g * l1[0] + h * l1[1] + i * l1[2];

    nl2[0] = a * l2[0] + b * l2[1] + c * l2[2];
    nl2[1] = d * l2[0] + e * l2[1] + f * l2[2];
    nl2[2] = g * l2[0] + h * l2[1] + i * l2[2];

    nr1[0] = a * r1[0] + b * r1[1] + c * r1[2];
    nr1[1] = d * r1[0] + e * r1[1] + f * r1[2];
    nr1[2] = g * r1[0] + h * r1[1] + i * r1[2];

    nr2[0] = a * r2[0] + b * r2[1] + c * r2[2];
    nr2[1] = d * r2[0] + e * r2[1] + f * r2[2];
    nr2[2] = g * r2[0] + h * r2[1] + i * r2[2];

    l1 = nl1;
    l2 = nl2;
    r1 = nr1;
    r2 = nr2;
  }

  public float[][] getUniform() {
    float[][] out = {
        pos,
        l1,
        l2,
        r1,
        r2
    };
    return out;
  }

}
