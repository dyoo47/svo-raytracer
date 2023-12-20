package src.tests;

import org.junit.Test;
import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL43C.*;

import java.nio.ShortBuffer;

import src.engine.*;

public class TestApp extends Application {

  @Test
  public void test() {
    launch(new TestApp());
  }

  @Override
  public void preRun() {
    Renderer renderer = Renderer.getInstance();
    int chunkSize = Constants.CHUNK_SIZE;

    ShortBuffer testBuffer = BufferUtils.createShortBuffer(1024 * 1024);

    int texture = glGenTextures();
    glBindTexture(GL_TEXTURE_2D, texture);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexStorage2D(GL_TEXTURE_2D, 1, GL_R16UI, 1024, 1024);
    glBindImageTexture(4, texture, 0, true, 0, GL_READ_ONLY, GL_R16UI);
    renderer.printGLErrors();
    System.out.println("Adding 2d texture data");
    // glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, GL_R16UI, 1024, 1024, 0,
    // GL_RED_INTEGER, GL_UNSIGNED_SHORT, testBuffer);
    glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 1024, 1024, GL_RED_INTEGER, GL_UNSIGNED_SHORT, testBuffer);
    renderer.printGLErrors();
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
