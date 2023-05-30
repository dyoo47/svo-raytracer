import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL.createCapabilities;
import static org.lwjgl.opengl.GL43C.*;
import static org.lwjgl.opengl.GLUtil.setupDebugMessageCallback;
import static org.lwjgl.system.MemoryUtil.*;

import java.nio.IntBuffer;
import java.util.Objects;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL32;
import org.lwjgl.system.MemoryStack;



public class GuiManager{

  public static void main(String[] args){

    final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();

    if (!glfwInit())
        throw new IllegalStateException("Unable to initialize GLFW");
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
    glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
    glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
    glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);
    long window = glfwCreateWindow(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT, "svoraytracer", NULL, NULL);
    if (window == NULL)
        throw new AssertionError("Failed to create the GLFW window");

    try (MemoryStack stack = MemoryStack.stackPush()) {
      final IntBuffer pWidth = stack.mallocInt(1); // int*
      final IntBuffer pHeight = stack.mallocInt(1); // int*

      GLFW.glfwGetWindowSize(window, pWidth, pHeight);
      final GLFWVidMode vidmode = Objects.requireNonNull(GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor()));
      GLFW.glfwSetWindowPos(window, (vidmode.width() - pWidth.get(0)) / 2, (vidmode.height() - pHeight.get(0)) / 2);
    }

    glfwMakeContextCurrent(window);
    createCapabilities();

    ImGui.createContext();
    imGuiGlfw.init(window, true);
    imGuiGl3.init("#version 430");
    clearBuffer();

    // Make window visible and loop until window should be closed
    glfwShowWindow(window);
    while (!glfwWindowShouldClose(window)) {
      glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer
      imGuiGlfw.newFrame();
      ImGui.newFrame();
      //Do stuff
      ImGui.text("hello there");

      ImGui.render();
      imGuiGl3.renderDrawData(ImGui.getDrawData());

      if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
          final long backupWindowPtr = GLFW.glfwGetCurrentContext();
          ImGui.updatePlatformWindows();
          ImGui.renderPlatformWindowsDefault();
          GLFW.glfwMakeContextCurrent(backupWindowPtr);
      }

			glfwSwapBuffers(window);
      glfwPollEvents();
    }
  }
  
  private static void clearBuffer() {
    GL32.glClearColor(0.5f, 0.5f, 1.0f, 1.0f);
    GL32.glClear(GL32.GL_COLOR_BUFFER_BIT | GL32.GL_DEPTH_BUFFER_BIT);
  }
}
