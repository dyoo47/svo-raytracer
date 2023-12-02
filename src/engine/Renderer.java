import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import static org.lwjgl.opengl.GL43C.*;

public class Renderer {

  private static final Renderer instance = new Renderer();
  private ArrayList<Shader> shaders;

  public class Shader {
    String name;
    int computeProgram;
    int computeProgramShader;
    Shader(String name, int computeProgram, int computeProgramShader){
      this.name = name;
      this.computeProgram = computeProgram;
      this.computeProgramShader = computeProgramShader;
    }
  }

  private Renderer(){
    shaders = new ArrayList<Shader>();
  }

  public static Renderer getInstance(){
    return instance;
  }

  public Shader addShader(String name, String path){
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

  public void dispatchCompute(Shader shader, int numGroupsX, int numGroupsY, int numGroupsZ){
    glUseProgram(shader.computeProgram);
    glDispatchCompute(numGroupsX, numGroupsY, numGroupsZ);
    glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
  }

  public void addSSBO(String name, Shader shader, int bindIndex, ByteBuffer data){
    int ssbo = glGenBuffers();
    int blockIndex = glGetProgramResourceIndex(shader.computeProgram, GL_SHADER_STORAGE_BLOCK, name);
    glShaderStorageBlockBinding(shader.computeProgram, blockIndex, bindIndex);
    glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbo);
    glBindBufferRange(GL_SHADER_STORAGE_BUFFER, bindIndex, ssbo, 0, 3);
    glBufferData(GL_SHADER_STORAGE_BUFFER, data, GL_DYNAMIC_DRAW);
    glBindBufferBase(GL_SHADER_STORAGE_BUFFER, bindIndex, ssbo);
  }

  public void updateSSBO(int bindIndex, ByteBuffer data){
    glBindBuffer(GL_SHADER_STORAGE_BUFFER, bindIndex);
    glBufferData(GL_SHADER_STORAGE_BUFFER, data, GL_DYNAMIC_DRAW);
  }

  public Shader getShaderByName(String name){
    for(Shader shader : shaders){
      if(shader.name.equals(name)) return shader;
    }
    return null;
  }

  public static String readFromFile(String filePath){
    String content = "";
    try{
      content = Files.readString(Paths.get(filePath));
    } catch (IOException e){
      e.printStackTrace();
    }
    return content;
  }
}
