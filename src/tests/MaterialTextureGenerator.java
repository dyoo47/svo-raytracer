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

  class Material {
    byte id;
    String mapFilePath;
    public Material(byte id, String mapFilePath){
      this.id = id;
      this.mapFilePath = mapFilePath;
    }
  }

  @Test
  public void test(){
    Configuration.STACK_SIZE.set(3072); // This is in kb
    launch(new MaterialTextureGenerator());
  }

  @Override
  public void preRun(){
    int textureSize = 1024;
    final int numMaterials = 3;
    boolean generateVisual = true;
    String outputPath = "./assets/matmaps/materials.png";
    String outputVisPath = "./assets/matmaps/materials.vis.png";
    Material[] materials = new Material[numMaterials];
    materials[0] = new Material((byte) 1, "./assets/matmaps/stone.png");
    materials[1] = new Material((byte) 2, "./assets/matmaps/scree.png");
    materials[2] = new Material((byte) 3, "./assets/matmaps/grass.png");
    ByteBuffer combinedBuffer;
    ByteBuffer visualBuffer = null;
    try(MemoryStack stack = MemoryStack.stackPush()){
      combinedBuffer = stack.malloc(textureSize * textureSize);
      if(generateVisual) visualBuffer = stack.malloc(textureSize * textureSize);
      BufferUtils.zeroBuffer(combinedBuffer);
      for(Material material : materials){
        ShortBuffer texBuffer = loadTexture(material.mapFilePath, stack);
        System.out.println("Loaded texture " + material.mapFilePath);
        for(int i=0; i < texBuffer.limit(); i++){
          if(texBuffer.get(i) == (byte) -1){
            combinedBuffer.put(i, (byte)(material.id));
            if(generateVisual){
              visualBuffer.put(i, (byte)(material.id * 16));
            }
          }
        }
        STBImage.stbi_image_free(texBuffer);
      }
    }
    STBImageWrite.stbi_write_png(outputPath, textureSize, textureSize, 1, combinedBuffer, textureSize);
    System.out.println("Successfully wrote to " + outputPath);
    if(generateVisual){
      STBImageWrite.stbi_write_png(outputVisPath, textureSize, textureSize, 1, visualBuffer, textureSize);
      System.out.println("Successfully wrote to " + outputVisPath);
    } 
  }

  private ShortBuffer loadTexture(String filePath, MemoryStack stack){
    ShortBuffer buffer = null;
    IntBuffer width = stack.mallocInt(1);
    IntBuffer height = stack.mallocInt(1);
    IntBuffer channels = stack.mallocInt(1);

    File heightmapFile = new File(filePath);
    String absoluteFilePath = heightmapFile.getAbsolutePath();
    buffer = STBImage.stbi_load_16(absoluteFilePath, width, height, channels, 1);
    if(buffer == null){
      System.out.println("Can't read file " + filePath + ": " + STBImage.stbi_failure_reason());
    }
    // STBImage.stbi_image_free(buffer);
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

