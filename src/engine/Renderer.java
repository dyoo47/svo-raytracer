import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import static org.lwjgl.opengl.GL43C.*;

public class Renderer {

  private static final Renderer instance = new Renderer();
  private ArrayList<Shader> shaders;

  private class Shader {
    String source;
    int computeProgram;
    int computeProgramShader;
    Shader(String source, int computeProgram, int computeProgramShader){
      this.source = source;
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

  public void addShader(String source){
    int computeProgram = glCreateProgram();
    int computeProgramShader = glCreateShader(GL_COMPUTE_SHADER);
    glShaderSource(computeProgramShader, source);
    glCompileShader(computeProgramShader);
    glAttachShader(computeProgram, computeProgramShader);
    glLinkProgram(computeProgram);

    shaders.add(new Shader(source, computeProgram, computeProgramShader));
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
