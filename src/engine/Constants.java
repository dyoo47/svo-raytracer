public class Constants {
    public static final int WINDOW_WIDTH = 1920;
    public static final int WINDOW_HEIGHT = 1080;
    public static final int CACHE_SIZE_MB = 1024;
    public static final int REQUEST_BUFFER_SIZE_KB = 1024;
    public static final float CAMERA_SENSITIVITY = 0.002f;
    public static final float CAMERA_LOWER_LIMIT = -1.570f;
    public static final float CAMERA_UPPER_LIMIT = 1.570f;
    public static final int OCTREE_MEMORY_SIZE_KB = 1000000;
    public static final int CHUNK_SIZE = 1024;
    public static final String MAP_DIR = "./assets/";
    public static final int[][] CHILD_OFFSETS = {
        {0, 0, 0},
        {1, 0, 0},
        {0, 1, 0},
        {1, 1, 0},
        {0, 0, 1},
        {1, 0, 1},
        {0, 1, 1},
        {1, 1, 1}
    };

    public static final int[] WORLD_OFFSET = {0, -512, 0};

    class Math{
        public static final float PI = 3.14159265359f;
    }
}
