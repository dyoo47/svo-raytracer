package src.engine;

import static org.lwjgl.opengl.GL43C.*;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;

import imgui.ImGui;
import src.engine.Octree.ChangeBounds;
import src.engine.sdf.Box;
import src.engine.sdf.SignedDistanceField;
import src.engine.sdf.Sphere;

public class Main extends Application {

  private static int frameNumber = 1;
  private static final String QUAD_PROGRAM_VS_SOURCE = Renderer.readFromFile("src/shaders/quad.vert");
  private static final String QUAD_PROGRAM_FS_SOURCE = Renderer.readFromFile("src/shaders/quad.frag");

  Renderer renderer;

  int quadProgram;
  Renderer.Shader traceShader;
  Renderer.Shader genShader;
  Renderer.Shader beamShader;

  int numGroupsX, numGroupsY;
  int renderMode, lastOffset;

  Camera cam;
  Octree octree;
  int[] voxelSpacePos;

  ByteBuffer buffer;

  int framebuffer;
  int depthbuffer;
  int beambuffer;
  int voxelTexture;

  int beamSquareSize = 4;

  ByteBuffer pixels;
  int[] frameWidth;
  int[] frameHeight;
  byte[] pixel;
  float crosshairDepth;

  boolean dirty = false;
  boolean showDebugWindow = false;
  boolean useBeamOptimization = false;

  @Override
  protected void preRun() {

    Material.initMaterials();
    renderer = Renderer.getInstance();

    pixels = BufferUtils.createByteBuffer(Constants.WINDOW_WIDTH * Constants.WINDOW_HEIGHT * 4);
    pixel = new byte[4];

    // Create VAO
    glBindVertexArray(glGenVertexArrays());

    // Create framebuffer texture to render into
    framebuffer = glGenTextures();
    glBindTexture(GL_TEXTURE_2D, framebuffer);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA8, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
    glBindImageTexture(0, framebuffer, 0, false, 0, GL_WRITE_ONLY, GL_RGBA8);

    // Create depthbuffer texture to store depth data
    depthbuffer = glGenTextures();
    glBindTexture(GL_TEXTURE_2D, depthbuffer);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexStorage2D(GL_TEXTURE_2D, 1, GL_R32F, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
    glBindImageTexture(1, depthbuffer, 0, false, 0, GL_WRITE_ONLY, GL_R32UI);

    if (useBeamOptimization) {
      beambuffer = glGenTextures();
      glBindTexture(GL_TEXTURE_2D, beambuffer);
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
      glTexStorage2D(GL_TEXTURE_2D, 1, GL_R32F, Constants.WINDOW_WIDTH / beamSquareSize,
          Constants.WINDOW_HEIGHT / beamSquareSize);
      glBindImageTexture(2, beambuffer, 0, false, 0, GL_WRITE_ONLY, GL_R32F);
    }
    // Create program to render framebuffer texture as fullscreen quad
    System.out.print("creating fullscreen quad...");
    quadProgram = glCreateProgram();
    int quadProgramVs = glCreateShader(GL_VERTEX_SHADER);
    int quadProgramFs = glCreateShader(GL_FRAGMENT_SHADER);
    glShaderSource(quadProgramVs, QUAD_PROGRAM_VS_SOURCE);
    glShaderSource(quadProgramFs, QUAD_PROGRAM_FS_SOURCE);
    glCompileShader(quadProgramVs);
    glCompileShader(quadProgramFs);
    glAttachShader(quadProgram, quadProgramVs);
    glAttachShader(quadProgram, quadProgramFs);
    glLinkProgram(quadProgram);
    System.out.println(" done!");

    // Set up compute shaders
    traceShader = renderer.addShader("svotrace", "src/shaders/svotrace.comp");
    if (useBeamOptimization) {
      beamShader = renderer.addShader("beamShader", "src/shaders/svobeam.comp");
    }

    // Determine number of work groups to dispatch
    numGroupsX = (int) Math.ceil((double) Constants.WINDOW_WIDTH / 8);
    numGroupsY = (int) Math.ceil((double) Constants.WINDOW_HEIGHT / 8);

    // Make window visible and loop until window should be closed

    // --INITIALIZE
    System.out.print("creating voxel data...");
    octree = new Octree(Constants.OCTREE_MEMORY_SIZE_KB);
    octree.readBufferFromFile("debug.svo");
    System.out.println(" done!");

    cam = new Camera();
    cam.setPos(1.5f, 1.5f, 2.0f);

    renderer.addSSBO(7, octree.getByteBuffer());
    // renderer.bindSSBO("shaderStorage", beamShader, 7);

    renderMode = 2;
    lastOffset = 0;
  }

  @Override
  public void updateEarly() {

    glBindTexture(GL_TEXTURE_2D, depthbuffer);
    frameWidth = new int[1];
    frameHeight = new int[1];
    glGetTexLevelParameteriv(GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH, frameWidth);
    glGetTexLevelParameteriv(GL_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT, frameHeight);
    pixels.clear();
    glGetTexImage(GL_TEXTURE_2D, 0, GL_RED, GL_FLOAT, pixels);
    int row = 540 * Constants.WINDOW_WIDTH * 4;
    int col = 960 * 4;
    int offset = row + col;
    pixel[0] = pixels.get(0 + offset);
    pixel[1] = pixels.get(1 + offset);
    pixel[2] = pixels.get(2 + offset);
    pixel[3] = pixels.get(3 + offset);
    crosshairDepth = pixels.getFloat(offset);

    glBindTexture(GL_TEXTURE_2D, framebuffer);

    // if (lastOffset != octree.memOffset || dirty) {
    // dirty = false;
    // frameNumber = 0;
    // lastOffset = octree.memOffset;
    // double startTime = System.currentTimeMillis();
    // renderer.updateSSBO(7, octree.getByteBuffer());
    // double endTime = System.currentTimeMillis() - startTime;
    // System.out.println("Updated octree. Took " + endTime + " ms.");
    // }

    // Update camera position
    if (Input.keyDown(Input.MOVE_FORWARD)) {
      frameNumber = 0;
      cam.strafe(1, 0);
    }
    if (Input.keyDown(Input.MOVE_BACK)) {
      frameNumber = 0;
      cam.strafe(-1, 0);
    }
    if (Input.keyDown(Input.MOVE_LEFT)) {
      frameNumber = 0;
      cam.strafe(0, -1);
    }
    if (Input.keyDown(Input.MOVE_RIGHT)) {
      frameNumber = 0;
      cam.strafe(0, 1);
    }
    if (Input.keyDown(Input.MOVE_UP)) {
      frameNumber = 0;
      cam.setPos(cam.pos[0], cam.pos[1] + cam.speed, cam.pos[2]);
    }
    if (Input.keyDown(Input.MOVE_DOWN)) {
      frameNumber = 0;
      cam.setPos(cam.pos[0], cam.pos[1] - cam.speed, cam.pos[2]);
    }
    if (Input.keyPressed(Input.SAVE_WORLD)) {
      octree.writeBufferToFile("level1.svo");
    }
    if (Input.keyPressed(Input.READ_WORLD)) {
      octree.readBufferFromFile("level1.svo");
    }
    if (Input.keyPressed(Input.TOGGLE_DEBUG_WINDOW)) {
      showDebugWindow = !showDebugWindow;
    }
    if (Input.keyPressed(Input.TOGGLE_USE_BEAM)) {
      dirty = true;
      useBeamOptimization = !useBeamOptimization;
    }
    if (Input.keyDown(Input.RENDER_MODE_ZERO)) {
      renderMode = 0;
      frameNumber = 0;
    }
    if (Input.keyDown(Input.RENDER_MODE_ONE)) {
      renderMode = 1;
      frameNumber = 0;
    }
    if (Input.keyDown(Input.RENDER_MODE_TWO)) {
      renderMode = 2;
      frameNumber = 0;
    }
    if (Input.keyDown(Input.RENDER_MODE_THREE)) {
      renderMode = 3;
      frameNumber = 0;
    }
    if (Input.keyDown(Input.ROTATE_LEFT)) {
      frameNumber = 0;
      cam.rotate(0.0f, 0.01f, 0.0f);
    }
    if (Input.keyDown(Input.ROTATE_RIGHT)) {
      frameNumber = 0;
      cam.rotate(0.0f, -0.01f, 0.0f);
      // cam.rotateDir(0.0f, -0.01f);
    }
    if (Input.keyDown(Input.ROTATE_UP)) {
      frameNumber = 0;
      cam.rotate(0.01f, 0.0f, 0.0f);
    }
    if (Input.keyDown(Input.ROTATE_DOWN)) {
      frameNumber = 0;
      cam.rotate(-0.01f, 0.0f, 0.0f);
    }
    double[] mouseDelta = Input.getMouseDelta();
    if (mouseDelta[0] != 0 || mouseDelta[1] != 0)
      frameNumber = 0;
    cam.rotate(0.0f, (float) -mouseDelta[0] * Constants.CAMERA_SENSITIVITY, 0.0f);
    cam.rotate((float) -mouseDelta[1] * Constants.CAMERA_SENSITIVITY, 0.0f, 0.0f);

    if (Input.keyDown(Input.SPEED_TURBO)) {
      cam.setSpeed(0.005f);
    } else if (Input.keyDown(Input.SPEED_SLOW)) {
      cam.setSpeed(0.0001f);
    } else {
      cam.setSpeed(0.0005f);
    }
    if (Input.keyPressed(Input.REMOVE_NODE)) {
      dirty = true;
      System.out.println("Placed sphere at " + voxelSpacePos[0] + ", " + voxelSpacePos[1] + ", " + voxelSpacePos[2]);
      SignedDistanceField box = new Box(voxelSpacePos, 10, 20, 30);
      octree.useSDFBrush(box, (byte) 2);
    }
    if (Input.mouseButtonPressed(Input.SUBTRACT_SPHERE)) {
      placeSDF((byte) 0);
    }
    if (Input.mouseButtonPressed(Input.PUT_SPHERE)) {
      placeSDF((byte) 1);
    }

    if (useBeamOptimization) {
      renderer.useProgram(beamShader);
      glUniform1i(9, octree.memOffset);
      glUniform3fv(8, cam.pos);
      glUniform3fv(1, cam.l1);
      glUniform3fv(2, cam.l2);
      glUniform3fv(3, cam.r1);
      glUniform3fv(4, cam.r2);
      renderer.dispatchCompute(beamShader, numGroupsX / beamSquareSize, numGroupsY / beamSquareSize, 1);
    }
    renderer.useProgram(traceShader);

    glUniform3fv(8, cam.pos);
    glUniform3fv(1, cam.l1);
    glUniform3fv(2, cam.l2);
    glUniform3fv(3, cam.r1);
    glUniform3fv(4, cam.r2);
    // Update frame
    frameNumber++;
    glUniform1i(5, frameNumber);
    glUniform1i(6, renderMode);
    glUniform1i(9, octree.memOffset);

    if (useBeamOptimization)
      glUniform1i(11, 1);
    else
      glUniform1i(11, 0);

    renderer.dispatchCompute(traceShader, numGroupsX, numGroupsY, 1);
    // Display framebuffer texture
    glUseProgram(quadProgram);
    glDrawArrays(GL_TRIANGLES, 0, 3);
  }

  @Override
  public void drawUi() {
    if (showDebugWindow) {
      ImGui.text("Render Mode: " + renderMode);
      ImGui.text("Position: " + String.format("%.3f", cam.pos[0]) + ", " + String.format("%.3f", cam.pos[1]) + ", "
          + String.format("%.3f", cam.pos[2]));
      ImGui.text("Rotation: " + String.format("%.3f", cam.rot[0]) + ", " + String.format("%.3f", cam.rot[1]) + ", "
          + String.format("%.3f", cam.rot[2]));
      ImGui.text("Rotation: " + String.format("%.3f", cam.dir[0]) + ", " + String.format("%.3f", cam.dir[1]) + ", "
          + String.format("%.3f", cam.dir[2]));
      ImGui.text("Voxel Space Position: " + voxelSpacePos[0] + ", " + voxelSpacePos[1] + ", " + voxelSpacePos[2]);
      ImGui.text("Octree Size: " + octree.memOffset + " bytes");
      ImGui.text("Frame Time: " + frameTime + " ms");
      ImGui.text("Texture Width: " + frameWidth[0]);
      ImGui.text("Texture Height: " + frameHeight[0]);
      int[] lookAtPos = cam.getRayPickLocation(crosshairDepth);
      ImGui.text("LookAt Pos: " + lookAtPos[0] + ", " + lookAtPos[1] + ", " + lookAtPos[2]);
      if (useBeamOptimization) {
        ImGui.text("Beam Optimization: true");
      } else {
        ImGui.text("Beam Optimization: false");
      }
    }
  }

  @Override
  public void update() {
    voxelSpacePos = Util.toVoxelSpace(cam.pos);
  }

  @Override
  public void updateLate() {
    Input.update();
  }

  public static void main(String[] args) {
    launch(new Main());
  }

  static String ff(float value) {
    return String.format("%.3f", value);
  }

  static String fd(double value) {
    return String.format("%.0f", value);
  }

  private void placeSDF(byte value) {
    int[] targetPos = cam.getRayPickLocation(crosshairDepth);
    dirty = true;
    System.out.println("Placed sphere at " + targetPos[0] + ", " + targetPos[1] + ", " + targetPos[2]);
    double startTime = System.currentTimeMillis();
    SignedDistanceField sphere = new Sphere(targetPos, 64);
    ChangeBounds cb = octree.useSDFBrush(sphere, (byte) value);
    double endTime = System.currentTimeMillis() - startTime;
    System.out.println("Took " + endTime + "ms");
    System.out.println("Change bounds: [" + cb.start0 + ", " + cb.end0 + "] [" + cb.start1 + ", " + cb.end1 + "]");
    startTime = System.currentTimeMillis();
    renderer.updateSSBO(7, octree.getByteBuffer(), cb.start0, cb.end0);
    renderer.updateSSBO(7, octree.getByteBuffer(), cb.start1, cb.end1);
    endTime = System.currentTimeMillis() - startTime;
    System.out.println("Buffer update took " + endTime + " ms");
  }
}