public class Constants {
    public static final int WINDOW_WIDTH = 1920;
    public static final int WINDOW_HEIGHT = 1080;
    public static final float CAMERA_SENSITIVITY = 0.002f;
    public static final float CAMERA_LOWER_LIMIT = -1.570f;
    public static final float CAMERA_UPPER_LIMIT = 1.570f;
    public static final int[][] childOffsets = {
        {0, 0, 0},
        {1, 0, 0},
        {0, 1, 0},
        {1, 1, 0},
        {0, 0, 1},
        {1, 0, 1},
        {0, 1, 1},
        {1, 1, 1}
    };

    class Math{
        public static final float PI = 3.14159265359f;
    }
}
