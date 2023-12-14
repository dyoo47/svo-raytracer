package src.tests;

import java.nio.ByteBuffer;
import org.junit.Test;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL43C.*;
import src.engine.*;

public class WorldGenerator extends Application {

  @Test
  public void test(){
    launch(new WorldGenerator());
  }

  @Override
  public void preRun(){
    Renderer renderer = Renderer.getInstance();
    int chunkSize = Constants.CHUNK_SIZE;
    int texture = renderer.add3DTexture(3, GL_R8I, chunkSize, chunkSize, chunkSize);
    System.out.println("Added 3d texture");

    Renderer.Shader chunkGenShader = renderer.addShader("chunkgen", "src/shaders/chunkgen.comp");
    System.out.println("Added shaders");

    int groupSize = chunkSize/8;

    renderer.useProgram(chunkGenShader);

    renderer.setUniformInteger(1, 0);
    renderer.setUniformInteger(2, -1024);
    renderer.setUniformInteger(3, 0);

    renderer.printGLErrors();
    renderer.dispatchCompute(chunkGenShader, groupSize, groupSize, groupSize);

    ByteBuffer voxelData = BufferUtils.createByteBuffer(chunkSize * chunkSize * chunkSize);

    renderer.printGLErrors();
    renderer.get3DTextureData(texture, voxelData);

    Octree eo = new Octree(Constants.OCTREE_MEMORY_SIZE_KB);
    eo.constructCompleteOctree(chunkGenShader, texture);
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
