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

    Renderer.Shader shader = renderer.addShader("chunkgen", "src/shaders/chunkgen.comp");
    System.out.println("Added shader");

    // renderer.printGLErrors();
    // renderer.dispatchCompute(shader, width/groupSize, height/groupSize, depth/groupSize);
    // System.out.println("Dispatched compute");

    // renderer.printGLErrors();
    // renderer.get3DTextureData(texture, buffer);
    // System.out.println("Retrieved 3d texture data");

    EfficientOctree eo = new EfficientOctree(1000000, 2048, Constants.WORLD_OFFSET);
    eo.constructDebugOctree(shader, texture);
    eo.writeBufferToFile("debug.svo");
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
