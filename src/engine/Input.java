package src.engine;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.system.libffi.FFICIF;

import static org.lwjgl.glfw.GLFW.*;

import java.nio.DoubleBuffer;
import java.util.ArrayList;

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
    public static final int SPEED_TURBO = GLFW_KEY_LEFT_SHIFT;
    public static final int SPEED_SLOW = GLFW_KEY_LEFT_CONTROL;
    public static final int RENDER_MODE_ZERO = GLFW_KEY_1;
    public static final int RENDER_MODE_ONE = GLFW_KEY_2;
    public static final int RENDER_MODE_TWO = GLFW_KEY_3;
    public static final int RENDER_MODE_THREE = GLFW_KEY_4;

    public static final int REMOVE_NODE = GLFW_KEY_APOSTROPHE;
    public static final int SUBTRACT_SPHERE = GLFW_MOUSE_BUTTON_2;
    public static final int PUT_SPHERE = GLFW_MOUSE_BUTTON_1;
    public static final int SAVE_WORLD = GLFW_KEY_0;
    public static final int READ_WORLD = GLFW_KEY_9;

    public static final int TOGGLE_DEBUG_WINDOW = GLFW_KEY_GRAVE_ACCENT;
    public static final int TOGGLE_USE_BEAM = GLFW_KEY_BACKSLASH;

    static long window;
    static double prevX;
    static double prevY;

    private static boolean[] keybinds = new boolean[349];
    private static boolean[] mouseButtonBinds = new boolean[7];
    private static ArrayList<Integer> keybindsPressed;
    private static ArrayList<Integer> mouseButtonsPressed;

    public static void init(long windowPtr) {
        keybindsPressed = new ArrayList<Integer>();
        mouseButtonsPressed = new ArrayList<Integer>();
        prevX = 0;
        prevY = 0;
        window = windowPtr;
    }

    public static void update() {
        keybindsPressed.clear();
        mouseButtonsPressed.clear();
    }

    public static void setKeybinds(long window) {
        glfwSetKeyCallback(window, new GLFWKeyCallback() {
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (action == GLFW_RELEASE && key == GLFW_KEY_ESCAPE)
                    glfwSetWindowShouldClose(window, true);
                if (action == GLFW_PRESS) {
                    keybinds[key] = true;
                    keybindsPressed.add(key);
                } else if (action == GLFW_RELEASE) {
                    keybinds[key] = false;
                }
            }

            @Override
            public void callback(long arg0, long arg1) {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'callback'");
            }

            @Override
            public FFICIF getCallInterface() {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'getCallInterface'");
            }
        });

        glfwSetMouseButtonCallback(window, new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                if (action == GLFW_PRESS) {
                    mouseButtonBinds[button] = true;
                    mouseButtonsPressed.add(button);
                } else if (action == GLFW_RELEASE) {
                    mouseButtonBinds[button] = false;
                }
            }
        });
    }

    public static boolean keyDown(int key) {
        return keybinds[key];
    }

    public static boolean getMouseButtonDown(int button) {
        return mouseButtonBinds[button];
    }

    public static boolean keyPressed(int key) {
        return keybindsPressed.contains(key);
    }

    public static boolean mouseButtonPressed(int button) {
        return mouseButtonsPressed.contains(button);
    }

    public static double[] getMouseDelta() {
        DoubleBuffer mouseXBuffer = BufferUtils.createDoubleBuffer(1);
        DoubleBuffer mouseYBuffer = BufferUtils.createDoubleBuffer(1);
        glfwGetCursorPos(window, mouseXBuffer, mouseYBuffer);
        mouseXBuffer.rewind();
        mouseYBuffer.rewind();
        double newX = mouseXBuffer.get();
        double newY = mouseYBuffer.get();
        double deltaX = newX - (Constants.WINDOW_WIDTH / 2);
        double deltaY = newY - (Constants.WINDOW_HEIGHT / 2);
        double[] out = { deltaX, deltaY };
        glfwSetCursorPos(window, Constants.WINDOW_WIDTH / 2, Constants.WINDOW_HEIGHT / 2);
        return out;
    }
}
