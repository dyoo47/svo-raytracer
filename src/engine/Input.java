import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import static org.lwjgl.glfw.GLFW.*;

import java.nio.DoubleBuffer;

public class Input {

    public static final int MOVE_FORWARD = GLFW_KEY_W;
    public static final int MOVE_LEFT = GLFW_KEY_A;
    public static final int MOVE_RIGHT = GLFW_KEY_D;
    public static final int MOVE_BACK = GLFW_KEY_S;
    public static final int MOVE_UP = GLFW_KEY_Q;
    public static final int MOVE_DOWN = GLFW_KEY_E;
    public static final int ROTATE_LEFT = GLFW_KEY_LEFT;
    public static final int ROTATE_RIGHT = GLFW_KEY_RIGHT;
    public static final int ROTATE_DOWN = GLFW_KEY_DOWN;
    public static final int ROTATE_UP = GLFW_KEY_UP;
    long window;
    double prevX;
    double prevY;

    public Input(long window){
        prevX = 0;
        prevY = 0;
        this.window = window;
    }

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

    public double[] getMouseDelta(){
        DoubleBuffer mouseXBuffer = BufferUtils.createDoubleBuffer(1);
        DoubleBuffer mouseYBuffer = BufferUtils.createDoubleBuffer(1);
        glfwGetCursorPos(window, mouseXBuffer, mouseYBuffer);
        mouseXBuffer.rewind();
        mouseYBuffer.rewind();
        double newX = mouseXBuffer.get();
        double newY = mouseYBuffer.get();
        double deltaX = newX - (Constants.WINDOW_WIDTH / 2);
        double deltaY = newY - (Constants.WINDOW_HEIGHT / 2);
        double[] out = {deltaX, deltaY};
        glfwSetCursorPos(window, Constants.WINDOW_WIDTH / 2, Constants.WINDOW_HEIGHT / 2);
        return out;
    }
}
