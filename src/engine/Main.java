import org.lwjgl.BufferUtils;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL.createCapabilities;
import static org.lwjgl.opengl.GL43C.*;
import static org.lwjgl.opengl.GLUtil.setupDebugMessageCallback;
import static org.lwjgl.system.MemoryUtil.*;


import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class Main {

    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 800;
    private static int frameNumber = 1;
    private static final String QUAD_PROGRAM_VS_SOURCE = Shader.readFromFile("src/shaders/quad.vert");
    private static final String QUAD_PROGRAM_FS_SOURCE = Shader.readFromFile("src/shaders/quad.frag");
    private static final String COMPUTE_SHADER_SOURCE = Shader.readFromFile("src/shaders/svotrace.comp");


  public static void main(String[] args) {
    System.out.println("initializing...");
    // Initialize GLFW and create window
    if (!glfwInit())
        throw new IllegalStateException("Unable to initialize GLFW");
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
    glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
    glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
    glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);
    long window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "svoraytracer", NULL, NULL);
    if (window == NULL)
        throw new AssertionError("Failed to create the GLFW window");
    Input.setKeybinds(window);

    // Make context current and install debug message callback
    glfwMakeContextCurrent(window);
    createCapabilities();
    setupDebugMessageCallback();

    // Create VAO
    glBindVertexArray(glGenVertexArrays());

    // Create framebuffer texture to render into
    int framebuffer = glGenTextures();
    glBindTexture(GL_TEXTURE_2D, framebuffer);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA8, WINDOW_WIDTH, WINDOW_HEIGHT);
    glBindImageTexture(0, framebuffer, 0, false, 0, GL_WRITE_ONLY, GL_RGBA8);

    // Create program to render framebuffer texture as fullscreen quad
    System.out.print("creating fullscreen quad...");
    int quadProgram = glCreateProgram();
    int quadProgramVs = glCreateShader(GL_VERTEX_SHADER);
    int quadProgramFs = glCreateShader(GL_FRAGMENT_SHADER);
    glShaderSource(quadProgramVs, QUAD_PROGRAM_VS_SOURCE);
    glShaderSource(quadProgramFs, QUAD_PROGRAM_FS_SOURCE);
    glCompileShader(quadProgramVs);
    glCompileShader(quadProgramFs);
    glAttachShader(quadProgram, quadProgramVs);
    glAttachShader(quadProgram, quadProgramFs);
    glLinkProgram(quadProgram);
    System.out.println("done!");

    // Create ray tracing compute shader
    System.out.print("setting up ray tracing compute shader...");
    int computeProgram = glCreateProgram();
    int computeProgramShader = glCreateShader(GL_COMPUTE_SHADER);
    glShaderSource(computeProgramShader, COMPUTE_SHADER_SOURCE);
    glCompileShader(computeProgramShader);
    glAttachShader(computeProgram, computeProgramShader);
    glLinkProgram(computeProgram);
    System.out.println("done!");
    // Determine number of work groups to dispatch
    int numGroupsX = (int) Math.ceil((double)WINDOW_WIDTH / 8);
    int numGroupsY = (int) Math.ceil((double)WINDOW_HEIGHT / 8);

    // Make window visible and loop until window should be closed
    glfwShowWindow(window);

    //--INITIALIZE
    //System.out.print("creating voxel data...");
    VoxelData voxelData = new VoxelData(128, 128, 128);
    voxelData.sampleMod(0, 0, 0);
    //Octree octree = new Octree(800, (byte)64, voxelData);
    //octree.constructOctree(voxelData, 4, 0, 0);
    EfficientOctree eo = new EfficientOctree(10000, (byte)1, voxelData);
    eo.constructOctree(voxelData, 6);
    //octree.logBuffer(32);


    IntBuffer out = BufferUtils.createIntBuffer(1);

    int ssbo = 0;
    ssbo = glGenBuffers();
    int bindIndex = 7;
    int blockIndex = glGetProgramResourceIndex(computeProgram, GL_SHADER_STORAGE_BLOCK, "shaderStorage");
    //ByteBuffer buffer = octree.getByteBuffer();
    glShaderStorageBlockBinding(computeProgram, blockIndex, bindIndex);
    glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbo);

    ByteBuffer buffer = eo.getByteBuffer();
    //System.out.println("--------------------");
    //int childPointer = (buffer.get(1) << 24) + (buffer.get(2) << 16) + (buffer.get(3) << 8) + (buffer.get(4));
    //System.out.println(buffer.get(1) + ", " + buffer.get(2) + ", " + buffer.get(3));
    // System.out.println("mem offset: " + eo.memOffset);
    // for(int i=0; i < 100; i++){
    //   System.out.println(i + ", " + buffer.get(i));
    // }
    
    //int[] testbuffer = {0, 0, 0x00000007, 0x0000000c};
    //for(int i=0; i < 4; i++){
      //System.out.println(testbuffer[i]);
    //}
    //int testint = (testbuffer[0] << 24) + (testbuffer[1] << 16) + (testbuffer[2] << 8) + (testbuffer[3] << 0);
    //System.out.println(testint);
    // for(int i = 0; i < 32; i++){
    //   int r = i % 4;
    //   int index = i / 4;
    //   int converted = (testbuffer[index] & (0x000000ff << (r*8))) >> (r*8);
    //   System.out.println("BYTE CONVERSION: " + converted);
    // }
    
    Camera cam = new Camera();
    cam.setPos(0, 0, 20);
    
    glBindBufferRange(GL_SHADER_STORAGE_BUFFER, bindIndex, ssbo, 0, 3);
    //System.out.println("memoffset: " + octree.memOffset);
    //glBufferData(GL_SHADER_STORAGE_BUFFER, octree.getIntBuffer(), GL_DYNAMIC_DRAW);
    glBufferData(GL_SHADER_STORAGE_BUFFER, buffer, GL_DYNAMIC_DRAW);
    //System.out.println("test index: " + buffer.get(1));
    glBindBufferBase(GL_SHADER_STORAGE_BUFFER, bindIndex, ssbo);
    glGetBufferParameteriv(GL_SHADER_STORAGE_BUFFER, GL_BUFFER_SIZE, out);

    glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
    //System.out.println("buffer size: " + out.get(0));

    while (!glfwWindowShouldClose(window)) {
      glfwPollEvents();

      // Trace the scene
      glUseProgram(computeProgram);
      glDispatchCompute(numGroupsX, numGroupsY, 1);
      glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

      //Update frame
      frameNumber++;
      glUniform1i(5, frameNumber);
      glUniform1i(9, eo.memOffset);
      //Write octree buffer size to uniform
      //glUniform1i(6, octree.memOffset);

      //Update camera position
      if(Input.keyDown(Input.MOVE_FORWARD)){
        cam.setPos(cam.pos[0], cam.pos[1], cam.pos[2] - cam.speed);
      }
      if(Input.keyDown(Input.MOVE_BACK)){
        cam.setPos(cam.pos[0], cam.pos[1], cam.pos[2] + cam.speed);
      }
      if(Input.keyDown(Input.MOVE_LEFT)){
        cam.setPos(cam.pos[0] - cam.speed, cam.pos[1], cam.pos[2]);
      }
      if(Input.keyDown(Input.MOVE_RIGHT)){
        cam.setPos(cam.pos[0] + cam.speed, cam.pos[1], cam.pos[2]);
      }
      if(Input.keyDown(Input.MOVE_UP)){
        cam.setPos(cam.pos[0], cam.pos[1] + cam.speed, cam.pos[2]);
      }
      if(Input.keyDown(Input.MOVE_DOWN)){
        cam.setPos(cam.pos[0], cam.pos[1] - cam.speed, cam.pos[2]);
      }
      if(Input.keyDown(Input.ROTATE_LEFT)){
        cam.rotate(1);
      }
      if(Input.keyDown(Input.ROTATE_RIGHT)){
        cam.rotate(-1);
      }
      glUniform3fv(8, cam.pos);
      glUniform3fv(1, cam.getL1());
      glUniform3fv(2, cam.getL2());
      glUniform3fv(3, cam.getR1());
      glUniform3fv(4, cam.getR2());
      // Display framebuffer texture
      glUseProgram(quadProgram);
      glDrawArrays(GL_TRIANGLES, 0, 3);
      glfwSwapBuffers(window);
    }
  }
}