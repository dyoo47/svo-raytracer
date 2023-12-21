package src.tests;

import org.junit.Test;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryStack;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import src.engine.*;

public class MaterialTextureGenerator extends Application {

  @Test
  public void test() {
    Configuration.STACK_SIZE.set(801200); // This is in kb
    launch(new MaterialTextureGenerator());
  }

  @Override
  public void preRun() {

    Material.initMaterials();
    int textureSize = 8192;
    boolean generateVisual = true;
    String outputPath = "./assets/matmaps/nz/materials.png";
    String outputVisPath = "./assets/matmaps/nz/materials.vis.png";

    ByteBuffer combinedBuffer;
    ByteBuffer visualBuffer = null;
    try (MemoryStack stack = MemoryStack.stackPush()) {
      combinedBuffer = stack.malloc(textureSize * textureSize);
      if (generateVisual)
        visualBuffer = stack.malloc(textureSize * textureSize);
      BufferUtils.zeroBuffer(combinedBuffer);
      for (int i = 0; i < Material.getNumMats(); i++) {
        Material material = Material.getMaterial(i);
        if (!material.hasMatMap())
          continue;
        ShortBuffer texBuffer = loadTexture(material.matmapFilePath, stack);
        System.out.println("Loaded texture " + material.matmapFilePath);
        for (int j = 0; j < texBuffer.limit(); j++) {
          if (texBuffer.get(j) == (byte) -1) {
            combinedBuffer.put(j, (byte) (material.value));
            if (generateVisual) {
              visualBuffer.put(j, (byte) (material.value * 16));
            }
          }
        }
        STBImage.stbi_image_free(texBuffer);
      }
    }
    STBImageWrite.stbi_write_png(outputPath, textureSize, textureSize, 1, combinedBuffer, textureSize);
    System.out.println("Successfully wrote to " + outputPath);
    if (generateVisual) {
      STBImageWrite.stbi_write_png(outputVisPath, textureSize, textureSize, 1, visualBuffer, textureSize);
      System.out.println("Successfully wrote to " + outputVisPath);
    }
  }

  private ShortBuffer loadTexture(String filePath, MemoryStack stack) {
    ShortBuffer buffer = null;
    IntBuffer width = stack.mallocInt(1);
    IntBuffer height = stack.mallocInt(1);
    IntBuffer channels = stack.mallocInt(1);

    File heightmapFile = new File(filePath);
    String absoluteFilePath = heightmapFile.getAbsolutePath();
    buffer = STBImage.stbi_load_16(absoluteFilePath, width, height, channels, 1);
    if (buffer == null) {
      System.out.println("Can't read file " + filePath + ": " + STBImage.stbi_failure_reason());
    }
    return buffer;
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
