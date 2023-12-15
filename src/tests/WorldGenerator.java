package src.tests;

import org.junit.Test;
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

    Octree eo = new Octree(Constants.OCTREE_MEMORY_SIZE_KB);
    eo.constructCompleteOctree(chunkGenShader, texture);
    eo.printNodeCounts();
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
