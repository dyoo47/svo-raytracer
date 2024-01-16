package src.engine;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import static org.lwjgl.opengl.GL43C.*;

public class Renderer {

  private static final Renderer instance = new Renderer();
  private ArrayList<Shader> shaders;
  int voxelTexture;

  public class Shader {
    String name;
    int computeProgram;
    int computeProgramShader;

    Shader(String name, int computeProgram, int computeProgramShader) {
      this.name = name;
      this.computeProgram = computeProgram;
      this.computeProgramShader = computeProgramShader;
    }
  }

  private Renderer() {
    shaders = new ArrayList<Shader>();
  }

  public static Renderer getInstance() {
    return instance;
  }

  public void initVoxelTexture() {
    voxelTexture = instance.add3DTexture(0, GL_R8UI, 1024, 1024, 1024);
    instance.addShader("chunkgen", "src/shaders/chunkgen.comp");
  }

  public Shader addShader(String name, String path) {
    int computeProgram = glCreateProgram();
    int computeProgramShader = glCreateShader(GL_COMPUTE_SHADER);
    glShaderSource(computeProgramShader, readFromFile(path));
    glCompileShader(computeProgramShader);
    glAttachShader(computeProgram, computeProgramShader);
    glLinkProgram(computeProgram);

    Shader shader = new Shader(name, computeProgram, computeProgramShader);
    shaders.add(shader);
    return shader;
  }

  public void setUniformInteger(int location, int value) {
    glUniform1i(location, value);
  }

  public void activateTextureUnit(int unit) {
    glActiveTexture(GL_TEXTURE0 + unit);
  }

  public void bind2DTexture(int texture) {
    glBindTexture(GL_TEXTURE_2D, texture);
  }

  public int add2DTexture(int unit, int internalFormat, int width, int height) {
    activateTextureUnit(unit);
    int texture = glGenTextures();
    bind2DTexture(texture);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexStorage2D(GL_TEXTURE_2D, 1, internalFormat, width, height);
    glBindImageTexture(unit, texture, 0, true, 0, GL_READ_WRITE, internalFormat); // TODO: Change this to GL_READ_ONLY?

    return texture;
  }

  public void buffer2DTexture(int texture, int unit, int width, int height, ShortBuffer buffer) {
    activateTextureUnit(unit);
    bind2DTexture(texture);
    glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RED_INTEGER, GL_UNSIGNED_SHORT, buffer);
  }

  public void buffer2DTexture(int texture, int unit, int width, int height, ByteBuffer buffer) {
    activateTextureUnit(unit);
    bind2DTexture(texture);
    glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RED_INTEGER, GL_BYTE, buffer);
  }

  public void bind3DTexture(int texture) {
    glBindTexture(GL_TEXTURE_3D, texture);
  }

  public int add3DTexture(int unit, int internalFormat, int width, int height, int depth) {
    activateTextureUnit(unit);
    int texture = glGenTextures();
    bind3DTexture(texture);
    glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexStorage3D(GL_TEXTURE_3D, 1, internalFormat, width, height, depth);
    glBindImageTexture(unit, texture, 0, true, 0, GL_WRITE_ONLY, internalFormat);
    return texture;
  }

  public void get3DTextureData(int texture, int unit, ByteBuffer buffer) {
    activateTextureUnit(unit);
    glBindTexture(GL_TEXTURE_3D, texture);
    printGLErrors();
    glGetTexImage(GL_TEXTURE_3D, 0, GL_RED_INTEGER, GL_BYTE, buffer);
    printGLErrors();
  }

  public void useProgram(Shader shader) {
    glUseProgram(shader.computeProgram);
  }

  public void dispatchCompute(Shader shader, int numGroupsX, int numGroupsY, int numGroupsZ) {
    glDispatchCompute(numGroupsX, numGroupsY, numGroupsZ);
    glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
  }

  public void addSSBO(int bindIndex, ByteBuffer data) {
    int ssbo = glGenBuffers();
    glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbo);
    glBindBufferRange(GL_SHADER_STORAGE_BUFFER, bindIndex, ssbo, 0, 3);
    glBufferData(GL_SHADER_STORAGE_BUFFER, data, GL_DYNAMIC_DRAW);
    glBindBufferBase(GL_SHADER_STORAGE_BUFFER, bindIndex, ssbo);
  }

  public void updateSSBO(int bindIndex, ByteBuffer data) {
    glBindBuffer(GL_SHADER_STORAGE_BUFFER, bindIndex);
    glBufferData(GL_SHADER_STORAGE_BUFFER, data, GL_DYNAMIC_DRAW);
  }

  public void updateSSBO(int bindIndex, ByteBuffer data, int start, int end) {
    if (start >= end) {
      System.out.println("Update SSBO error: Invalid parameters.");
      return;
    }
    int oldLimit = data.limit();
    glBindBuffer(GL_SHADER_STORAGE_BUFFER, bindIndex);
    data.limit(end).position(start);
    glBufferSubData(GL_SHADER_STORAGE_BUFFER, start, data);
    data.limit(oldLimit);
  }

  public void getSSBO(ByteBuffer buffer) {
    glGetBufferSubData(GL_SHADER_STORAGE_BUFFER, 0, buffer);
  }

  public Shader getShaderByName(String name) {
    for (Shader shader : shaders) {
      if (shader.name.equals(name))
        return shader;
    }
    return null;
  }

  public void printGLErrors() {
    int err;
    while ((err = glGetError()) != GL_NO_ERROR) {
      System.out.println("OpenGL ERR: " + err);
    }
  }

  public static String readFromFile(String filePath) {
    String content = "";
    try {
      content = Files.readString(Paths.get(filePath));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return content;
  }
}
