import java.nio.ByteBuffer;

import org.junit.Test;
import org.lwjgl.BufferUtils;

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
    int texture = renderer.add3DTexture(3, GL_R8I, width, height, depth);
    System.out.println("Added 3d texture");

    Renderer.Shader chunkGenShader = renderer.addShader("chunkgen", "src/shaders/chunkgen.comp");
    System.out.println("Added shaders");

    //start test code
    int groupSize = 1024/8;

    renderer.useProgram(chunkGenShader);

    renderer.setUniformInteger(1, 0);
    renderer.setUniformInteger(2, -1024);
    renderer.setUniformInteger(3, 0);

    renderer.printGLErrors();
    renderer.dispatchCompute(chunkGenShader, groupSize, groupSize, groupSize);

    ByteBuffer voxelData = BufferUtils.createByteBuffer(1024 * 1024 * 1024);

    renderer.printGLErrors();
    renderer.get3DTextureData(texture, voxelData);

    EfficientOctree eo = new EfficientOctree(1000000, 2048, Constants.WORLD_OFFSET);
    eo.constructDebugOctree(chunkGenShader, texture);
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
