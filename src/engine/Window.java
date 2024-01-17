package src.engine;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL.createCapabilities;
import static org.lwjgl.system.MemoryUtil.*;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL32;

import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;

public abstract class Window {

  final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
  final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
  protected long window;

  private double startTime, endTime;

  protected double frameTime;

  protected void initWindow() {
    if (!glfwInit())
      throw new IllegalStateException("Unable to initialize GLFW");
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
    glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
    glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
    glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);
    window = glfwCreateWindow(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT, "svoraytracer", NULL, NULL);
    if (window == NULL)
      throw new AssertionError("Failed to create the GLFW window");

    // Make context current and install debug message callback
    glfwMakeContextCurrent(window);
    createCapabilities();

    glfwSwapInterval(1);
    GL32.glClearColor(0f, 0f, 0f, 0f);
    glfwSetWindowTitle(window, "svo-raytracer");

    // Initialize input
    Input.init(window);
    Input.setKeybinds(window);
    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

    // Initialize ImGui stuff
    initGui();

    // Make window visible and loop until window should be closed
    glfwShowWindow(window);

  }

  private void initGui() {
    ImGui.createContext();
    imGuiGlfw.init(window, true);
    imGuiGl3.init("#version 430");
  }

  protected void run() {
    while (!glfwWindowShouldClose(window)) {
      startFrame();
      updateEarly();
      update();
      updateLate();
      endFrame();
    }
  }

  private void clearBuffer() {
    GL32.glClearColor(0.5f, 0.5f, 0.5f, 1f);
    // GL32.glClear(GL32.GL_COLOR_BUFFER_BIT | GL32.GL_DEPTH_BUFFER_BIT);
    GL32.glClear(GL32.GL_COLOR_BUFFER_BIT);
  }

  protected void startFrame() {
    startTime = System.currentTimeMillis();
    clearBuffer();
    imGuiGlfw.newFrame();
    ImGui.newFrame();
  }

  protected void endFrame() {
    drawUi();
    ImGui.render();
    imGuiGl3.renderDrawData(ImGui.getDrawData());

    if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
      final long backupWindowPtr = GLFW.glfwGetCurrentContext();
      ImGui.updatePlatformWindows();
      ImGui.renderPlatformWindowsDefault();
      GLFW.glfwMakeContextCurrent(backupWindowPtr);
    }
    glfwPollEvents();
    GLFW.glfwSwapBuffers(window);
    endTime = System.currentTimeMillis();
    frameTime = endTime - startTime;
  }

  public void destroy() {
    ImGui.destroyContext();
    GLFW.glfwDestroyWindow(window);
    GLFW.glfwTerminate();
  }

  public abstract void updateEarly();

  public abstract void update();

  public abstract void updateLate();

  public abstract void drawUi();
}
