import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL43C.*;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;

import imgui.ImGui;

public class Main extends Application {

  private static int frameNumber = 1;
  private static final String QUAD_PROGRAM_VS_SOURCE = Shader.readFromFile("src/shaders/quad.vert");
  private static final String QUAD_PROGRAM_FS_SOURCE = Shader.readFromFile("src/shaders/quad.frag");
  private static final String COMPUTE_SHADER_SOURCE = Shader.readFromFile("src/shaders/svotrace.comp");

  int computeProgram;
  int computeProgramShader;
  int quadProgram;

  int numGroupsX, numGroupsY;
  int renderMode, lastOffset;

  World world;
  Camera cam;

  ByteBuffer buffer;

  int framebuffer;
  int pointerbuffer;
  ByteBuffer pixels;
  int[] frameWidth;
  int[] frameHeight;
  byte[] pixel;
  int voxelPointer;

  @Override
  protected void preRun() {

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

    //Create pointerbuffer texture to store voxel pointer data
    pointerbuffer = glGenTextures();
    glBindTexture(GL_TEXTURE_2D, pointerbuffer);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexStorage2D(GL_TEXTURE_2D, 1, GL_R32UI, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
    glBindImageTexture(1, pointerbuffer, 0, false, 0, GL_WRITE_ONLY, GL_R32UI);


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

    // Create ray tracing compute shader
    System.out.print("setting up ray tracing compute shader...");
    computeProgram = glCreateProgram();
    computeProgramShader = glCreateShader(GL_COMPUTE_SHADER);
    glShaderSource(computeProgramShader, COMPUTE_SHADER_SOURCE);
    glCompileShader(computeProgramShader);
    glAttachShader(computeProgram, computeProgramShader);
    glLinkProgram(computeProgram);
    System.out.println(" done!");
    // Determine number of work groups to dispatch
    numGroupsX = (int) Math.ceil((double)Constants.WINDOW_WIDTH / 8);
    numGroupsY = (int) Math.ceil((double)Constants.WINDOW_HEIGHT / 8);

    // Make window visible and loop until window should be closed

    //--INITIALIZE
    System.out.print("creating voxel data...");
    world = new World(9, 1024);
    System.out.println(" done!");

    //IntBuffer out = BufferUtils.createIntBuffer(1);
    cam = new Camera();
    cam.setPos(1.5f, 1.5f, 2.0f);

    int ssbo = 0;
    ssbo = glGenBuffers();
    int bindIndex = 7;
    int blockIndex = glGetProgramResourceIndex(computeProgram, GL_SHADER_STORAGE_BLOCK, "shaderStorage");
    glShaderStorageBlockBinding(computeProgram, blockIndex, bindIndex);
    glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbo);

    buffer = world.octreeBuffer;
    System.out.println("mem offset: " + world.eo.memOffset);
    
    glBindBufferRange(GL_SHADER_STORAGE_BUFFER, bindIndex, ssbo, 0, 3);
    glBufferData(GL_SHADER_STORAGE_BUFFER, buffer, GL_DYNAMIC_DRAW);
    glBindBufferBase(GL_SHADER_STORAGE_BUFFER, bindIndex, ssbo);
    //glGetBufferParameteriv(GL_SHADER_STORAGE_BUFFER, GL_BUFFER_SIZE, out);
    //glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

    //glfwMaximizeWindow(window);
    renderMode = 0;
    lastOffset = 0;
  }

  @Override
  public void updateEarly() {
    glUseProgram(computeProgram);
    glDispatchCompute(numGroupsX, numGroupsY, 1);
    glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

    //glReadPixels(0, 0, 1, 1, GL_ALPHA, GL_UNSIGNED_BYTE, alpha);
    glBindTexture(GL_TEXTURE_2D, pointerbuffer);
    frameWidth = new int[1];
    frameHeight = new int[1];
    glGetTexLevelParameteriv(GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH, frameWidth);
    glGetTexLevelParameteriv(GL_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT, frameHeight);
    pixels.clear();
    glGetTexImage(GL_TEXTURE_2D, 0, GL_RED_INTEGER, GL_UNSIGNED_INT, pixels);
    int row = 540 * Constants.WINDOW_WIDTH * 4;
    int col = 960 * 4;
    int offset = row + col;
    // pixel[0] = Byte.toUnsignedInt(pixels.get(0 + offset));
    // pixel[1] = Byte.toUnsignedInt(pixels.get(1 + offset));
    // pixel[2] = Byte.toUnsignedInt(pixels.get(2 + offset));
    // pixel[3] = Byte.toUnsignedInt(pixels.get(3 + offset));
    pixel[0] = pixels.get(0 + offset);
    pixel[1] = pixels.get(1 + offset);
    pixel[2] = pixels.get(2 + offset);
    pixel[3] = pixels.get(3 + offset);
    //voxelPointer = ByteBuffer.wrap(pixel).getInt();
    voxelPointer = pixels.getInt(offset);
    glBindTexture(GL_TEXTURE_2D, framebuffer);

    //Update frame
    frameNumber++;
    glUniform1i(5, frameNumber);
    glUniform1i(6, renderMode);
    glUniform1i(9, world.eo.memOffset);
    //System.out.println("mem offset: " + world.eo.memOffset);
    if(lastOffset != world.eo.memOffset){
      //System.out.println("mem offset: " + world.eo.memOffset);
      frameNumber = 0;
      lastOffset = world.eo.memOffset;
      buffer = world.eo.getByteBuffer();
      glBufferData(GL_SHADER_STORAGE_BUFFER, buffer, GL_DYNAMIC_DRAW);
      //buffer = null;
    }
    //Write octree buffer size to uniform
    //glUniform1i(6, octree.memOffset);

    //Update camera position
    if(Input.keyDown(Input.MOVE_FORWARD)){
      frameNumber = 0;
      cam.strafe(1, 0);
    }
    if(Input.keyDown(Input.MOVE_BACK)){
      frameNumber = 0;
      cam.strafe(-1, 0);
    }
    if(Input.keyDown(Input.MOVE_LEFT)){
      frameNumber = 0;
      cam.strafe(0, -1);
    }
    if(Input.keyDown(Input.MOVE_RIGHT)){
      frameNumber = 0;
      cam.strafe(0, 1);
    }
    if(Input.keyDown(Input.MOVE_UP)){
      frameNumber = 0;
      cam.setPos(cam.pos[0], cam.pos[1] + cam.speed, cam.pos[2]);
    }
    if(Input.keyDown(Input.MOVE_DOWN)){
      frameNumber = 0;
      cam.setPos(cam.pos[0], cam.pos[1] - cam.speed, cam.pos[2]);
    }
    if(Input.keyDown(Input.RENDER_MODE_ZERO)){
      renderMode = 0;
      frameNumber = 0;
    }
    if(Input.keyDown(Input.RENDER_MODE_ONE)){
      renderMode = 1;
      frameNumber = 0;
    }
    if(Input.keyDown(Input.RENDER_MODE_TWO)){
      renderMode = 2;
      frameNumber = 0;
    }
    if(Input.keyDown(Input.RENDER_MODE_THREE)){
      renderMode = 3;
      frameNumber = 0;
    }
    if(Input.keyDown(Input.ROTATE_LEFT)){
      frameNumber = 0;
      cam.rotate(0.0f, 0.01f, 0.0f);
    }
    if(Input.keyDown(Input.ROTATE_RIGHT)){
      frameNumber = 0;
      cam.rotate(0.0f, -0.01f, 0.0f);
      //cam.rotateDir(0.0f, -0.01f);
    }
    if(Input.keyDown(Input.ROTATE_UP)){
      frameNumber = 0;
      cam.rotate(0.01f, 0.0f, 0.0f);
    }
    if(Input.keyDown(Input.ROTATE_DOWN)){
      frameNumber = 0;
      cam.rotate(-0.01f, 0.0f, 0.0f);
    }
    double[] mouseDelta = Input.getMouseDelta();
    if(mouseDelta[0] != 0 || mouseDelta[1] != 0) frameNumber = 0;
    cam.rotate(0.0f, (float)-mouseDelta[0] * Constants.CAMERA_SENSITIVITY, 0.0f);
    cam.rotate((float)-mouseDelta[1] * Constants.CAMERA_SENSITIVITY, 0.0f, 0.0f);

    if(Input.keyDown(Input.SPEED_TURBO)){
      cam.setSpeed(0.005f);
    }else if(Input.keyDown(Input.SPEED_SLOW)){
      cam.setSpeed(0.0001f);
    }else{
      cam.setSpeed(0.0005f);
    }


    glUniform3fv(8, cam.pos);
    glUniform3fv(1, cam.l1);
    glUniform3fv(2, cam.l2);
    glUniform3fv(3, cam.r1);
    glUniform3fv(4, cam.r2);
    // Display framebuffer texture
    glUseProgram(quadProgram);
    glDrawArrays(GL_TRIANGLES, 0, 3);
  }

  @Override
  public void drawUi() {
    ImGui.text("Render Mode: " + renderMode);
    ImGui.text("Position: " + String.format("%.3f", cam.pos[0]) + ", " + String.format("%.3f", cam.pos[1]) + ", " + String.format("%.3f", cam.pos[2]));
    ImGui.text("Octree Size: " + lastOffset + " bytes");
    ImGui.text("Frame Time: " + frameTime + " ms");
    ImGui.text("Texture Width: " + frameWidth[0]);
    ImGui.text("Texture Height: " + frameHeight[0]);
    ImGui.text("Voxel Pointer: " + pixel[0] + " " + pixel[1] + " " + pixel[2] + " " + pixel[3] + " -> " + voxelPointer);
  }
  
  @Override
  public void update() {
    
  }

  @Override
  public void updateLate() {
    
  }


  public static void main(String[] args) {
    launch(new Main());
  }
}