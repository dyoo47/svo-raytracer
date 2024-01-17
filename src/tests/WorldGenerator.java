package src.tests;

import org.junit.Test;
import org.lwjgl.system.Configuration;

import static org.lwjgl.opengl.GL43C.*;

import src.engine.*;

public class WorldGenerator extends Application {

  @Test
  public void test() {
    Configuration.STACK_SIZE.set(1501200); // This is in kb
    launch(new WorldGenerator());
  }

  @Override
  public void preRun() {
    Material.initMaterials();
    Renderer renderer = Renderer.getInstance();
    int chunkSize = Constants.CHUNK_SIZE;
    int textureSize = 8192;
    int voxelTexture = renderer.add3DTexture(3, GL_R8I, chunkSize, chunkSize, chunkSize);
    int heightmapTexture = renderer.add2DTexture(4, GL_R16UI, textureSize, textureSize);
    int materialTexture = renderer.add2DTexture(5, GL_R8I, textureSize, textureSize);
    System.out.println("Added textures");

    Renderer.Shader chunkGenShader = renderer.addShader("chunkgen", "src/shaders/chunkgen.comp");
    Renderer.Shader heightmapShader = renderer.addShader("heightmap", "src/shaders/chunkgen-heightmap.comp");
    System.out.println("Added shaders");

    renderer.printGLErrors();

    Octree eo = new Octree(Constants.OCTREE_MEMORY_SIZE_KB);
    // eo.constructCompleteOctree(chunkGenShader, voxelTexture);
    eo.constructCompleteOctree(heightmapShader, voxelTexture, heightmapTexture, materialTexture);
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
