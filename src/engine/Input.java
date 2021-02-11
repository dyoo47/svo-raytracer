package engine;
import org.lwjgl.glfw.*;
import static org.lwjgl.glfw.GLFW.*;

public class Input {

    public static final int MOVE_FORWARD = GLFW_KEY_W;
    public static final int MOVE_LEFT = GLFW_KEY_A;
    public static final int MOVE_RIGHT = GLFW_KEY_D;
    public static final int MOVE_BACK = GLFW_KEY_S;
    public static final int MOVE_UP = GLFW_KEY_Q;
    public static final int MOVE_DOWN = GLFW_KEY_E;
    public static final int ROTATE_LEFT = GLFW_KEY_R;
    public static final int ROTATE_RIGHT = GLFW_KEY_F;

    private static boolean[] keybinds = new boolean[349];

    public static void setKeybinds(long window){
        glfwSetKeyCallback(window, new GLFWKeyCallback() {
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (action == GLFW_RELEASE && key == GLFW_KEY_ESCAPE)
                    glfwSetWindowShouldClose(window, true);
                if(action == GLFW_PRESS){
                    keybinds[key] = true;
                }else if(action == GLFW_RELEASE){
                    keybinds[key] = false;
                }
            }
        });
    }

    public static boolean keyDown(int key){
        return keybinds[key];
    }
}
