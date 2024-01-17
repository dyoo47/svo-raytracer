package src.tests;

import org.junit.Test;
import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL43C.*;

import java.nio.ByteBuffer;

import src.engine.*;

public class TestApp extends Application {

  @Test
  public void test() {
    launch(new TestApp());
  }

  private static final String QUAD_PROGRAM_VS_SOURCE = Renderer.readFromFile("src/shaders/quad.vert");
  private static final String QUAD_PROGRAM_FS_SOURCE = Renderer.readFromFile("src/shaders/quad.frag");

  Renderer renderer;

  int quadProgram;

  int framebuffer;
  int pointerbuffer;
  int beambuffer;

  int numGroupsX, numGroupsY;

  int traceComputeProgram;
  int traceComputeProgramShader;
  int beamComputeProgram;
  int beamComputeProgramShader;

  @Override
  public void preRun() {
    renderer = Renderer.getInstance();
    // Create VAO
    glBindVertexArray(glGenVertexArrays());

    // Create framebuffer texture to render into
    framebuffer = glGenTextures();
    glBindTexture(GL_TEXTURE_2D, framebuffer);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA8, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
    glBindImageTexture(0, framebuffer, 0, false, 0, GL_WRITE_ONLY, GL_RGBA8);

    // Create pointerbuffer texture to store voxel pointer data
    pointerbuffer = glGenTextures();
    glBindTexture(GL_TEXTURE_2D, pointerbuffer);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexStorage2D(GL_TEXTURE_2D, 1, GL_R32UI, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
    glBindImageTexture(1, pointerbuffer, 0, false, 0, GL_WRITE_ONLY, GL_R32UI);

    beambuffer = glGenTextures();
    glBindTexture(GL_TEXTURE_2D, beambuffer);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexStorage2D(GL_TEXTURE_2D, 1, GL_R32F, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
    glBindImageTexture(2, beambuffer, 0, false, 0, GL_WRITE_ONLY, GL_R32F);

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

    ByteBuffer data = BufferUtils.createByteBuffer(1024);
    data.putInt(19);
    data.flip();

    traceComputeProgram = glCreateProgram();
    traceComputeProgramShader = glCreateShader(GL_COMPUTE_SHADER);
    glShaderSource(traceComputeProgramShader, Renderer.readFromFile("src/shaders/testtrace.comp"));
    glCompileShader(traceComputeProgramShader);
    glAttachShader(traceComputeProgram, traceComputeProgramShader);
    glLinkProgram(traceComputeProgram);

    beamComputeProgram = glCreateProgram();
    beamComputeProgramShader = glCreateShader(GL_COMPUTE_SHADER);
    glShaderSource(beamComputeProgramShader, Renderer.readFromFile("src/shaders/testbeam.comp"));
    glCompileShader(beamComputeProgramShader);
    glAttachShader(beamComputeProgram, beamComputeProgramShader);
    glLinkProgram(beamComputeProgram);

    int ssbo = glGenBuffers();
    glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbo);
    glBindBufferRange(GL_SHADER_STORAGE_BUFFER, 7, ssbo, 0, 3);
    glBufferData(GL_SHADER_STORAGE_BUFFER, data, GL_DYNAMIC_DRAW);
    glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 7, ssbo);

    numGroupsX = (int) Math.ceil((double) Constants.WINDOW_WIDTH / 8);
    numGroupsY = (int) Math.ceil((double) Constants.WINDOW_HEIGHT / 8);

    glUseProgram(traceComputeProgram);
    glDispatchCompute(numGroupsX, numGroupsY, 1);
    glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
    glUseProgram(beamComputeProgram);
    glDispatchCompute(numGroupsX, numGroupsY, 1);
  }

  @Override
  public void updateEarly() {

    glBindTexture(GL_TEXTURE_2D, framebuffer);
    glUseProgram(quadProgram);
    glDrawArrays(GL_TRIANGLES, 0, 3);
  }

  @Override
  public void update() {
  }

  @Override
  public void updateLate() {
  }

  @Override
  public void drawUi() {
  }

}
