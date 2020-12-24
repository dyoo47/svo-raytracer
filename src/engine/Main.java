package engine;

import org.lwjgl.glfw.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL.createCapabilities;
import static org.lwjgl.opengl.GL43C.*;
import static org.lwjgl.opengl.GLUtil.setupDebugMessageCallback;
import static org.lwjgl.system.MemoryUtil.*;

public class Main {

    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 800;
    private static int frameNumber = 1;
    private static final String QUAD_PROGRAM_VS_SOURCE = Shader.readFromFile("src/shaders/quad.vert");
    private static final String QUAD_PROGRAM_FS_SOURCE = Shader.readFromFile("src/shaders/quad.frag");
    private static final String COMPUTE_SHADER_SOURCE = Shader.readFromFile("src/shaders/svotrace.comp");


    public static void main(String[] args) {
        // Initialize GLFW and create window
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);
        long window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "svoraytracer", NULL, NULL);
        if (window == NULL)
            throw new AssertionError("Failed to create the GLFW window");
        glfwSetKeyCallback(window, new GLFWKeyCallback() {
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (action == GLFW_RELEASE && key == GLFW_KEY_ESCAPE)
                    glfwSetWindowShouldClose(window, true);
            }
        });

        // Make context current and install debug message callback
        glfwMakeContextCurrent(window);
        createCapabilities();
        setupDebugMessageCallback();

        // Create VAO
        glBindVertexArray(glGenVertexArrays());

        // Create framebuffer texture to render into
        int framebuffer = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, framebuffer);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA8, WINDOW_WIDTH, WINDOW_HEIGHT);
        glBindImageTexture(0, framebuffer, 0, false, 0, GL_WRITE_ONLY, GL_RGBA8);

        // Create program to render framebuffer texture as fullscreen quad
        int quadProgram = glCreateProgram();
        int quadProgramVs = glCreateShader(GL_VERTEX_SHADER);
        int quadProgramFs = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(quadProgramVs, QUAD_PROGRAM_VS_SOURCE);
        glShaderSource(quadProgramFs, QUAD_PROGRAM_FS_SOURCE);
        glCompileShader(quadProgramVs);
        glCompileShader(quadProgramFs);
        glAttachShader(quadProgram, quadProgramVs);
        glAttachShader(quadProgram, quadProgramFs);
        glLinkProgram(quadProgram);

        // Create ray tracing compute shader
        int computeProgram = glCreateProgram();
        int computeProgramShader = glCreateShader(GL_COMPUTE_SHADER);
        glShaderSource(computeProgramShader, COMPUTE_SHADER_SOURCE);
        glCompileShader(computeProgramShader);
        glAttachShader(computeProgram, computeProgramShader);
        glLinkProgram(computeProgram);
        // Determine number of work groups to dispatch
        int numGroupsX = (int) Math.ceil((double)WINDOW_WIDTH / 8);
        int numGroupsY = (int) Math.ceil((double)WINDOW_HEIGHT / 8);

        // Make window visible and loop until window should be closed
        glfwShowWindow(window);

        //--INITIALIZE--
        VoxelData voxelData = new VoxelData(32, 32, 32);
        voxelData.sampleSphere(0, 0, 0);
        Octree octree = new Octree(100, (byte)32, voxelData);
        octree.constructOctree(voxelData, 4, 0, 0);
        int tbo;
        int tboTex;
        tbo = glGenBuffers();
        glBindBuffer(GL_TEXTURE_BUFFER, tbo);
        glBufferData(GL_TEXTURE_BUFFER, octree.bufferSize, GL_DYNAMIC_DRAW);
        tboTex = glGenTextures();
        glBindBuffer(GL_TEXTURE_BUFFER, 0);

        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();

            // Trace the scene
            glUseProgram(computeProgram);
            glDispatchCompute(numGroupsX, numGroupsY, 1);
            glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

            //Update frame
            frameNumber++;
            glUniform1i(5, frameNumber);

            //Write octree to texture buffer used by compute shader
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_BUFFER, tboTex);
            glTexBuffer(GL_TEXTURE_BUFFER, GL_RG8I, tbo);
            glUniform1i(6, octree.memOffset);
            //glUniform1i(1, 0);

            // Display framebuffer texture
            glUseProgram(quadProgram);
            glDrawArrays(GL_TRIANGLES, 0, 3);
            glfwSwapBuffers(window);
        }
    }
}