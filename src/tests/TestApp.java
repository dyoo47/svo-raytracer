import java.nio.ByteBuffer;

import org.junit.Test;

import static org.lwjgl.opengl.GL43C.*;

public class TestApp extends Application {

  @Test
  public void test(){
    launch(new TestApp());
  }

  @Override
  public void preRun(){
    Renderer renderer = Renderer.getInstance();
    int width = 1024;
    int height = 1024;
    int depth = 1024;
    int texture = renderer.add3DTexture(3, GL_R8UI, width, height, depth);
    System.out.println("Added 3d texture");

    ByteBuffer buffer = ByteBuffer.allocateDirect(width * depth * height);

    Renderer.Shader shader = renderer.addShader("chunkgen", "src/shaders/chunkgen.comp");
    System.out.println("Added shader");

    renderer.printGLErrors();
    renderer.dispatchCompute(shader, width/8, height/8, depth/8);
    System.out.println("Dispatched compute");

    renderer.printGLErrors();
    renderer.dispatchCompute(shader, width/8, height/8, depth/8);
    renderer.get3DTextureData(texture, buffer);
    System.out.println("Retrieved 3d texture data");

    for(int i=0; i < 10; i++){
      System.out.println(buffer.get(i));
    }
  }

  @Override
  public void updateEarly() {
    
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
