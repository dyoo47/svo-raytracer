package engine;

public class Camera {
    float[] pos;
    float[] view;
    float[] up;
    float[] resolution;
    float[] fov;
    float apertureRadius;
    float focalDistance;

    class InteractiveCamera{
        float[] centerPosition;
        float[] viewDirection;
        float yaw;
        float pitch;
        float radius;
        float apertureRadius;
        float focalDistance;

        public float[] resolution;
        public float[] fov;

        public InteractiveCamera(){
            centerPosition = new float[]{0, 0, 0};
            yaw = 0;
            pitch = 0.3f;
            apertureRadius = 0.01f;
            focalDistance = 4.0f;

            resolution = new float[]{512, 512};
            fov = new float[]{40, 40};
        }

        public void buildRenderCamera(Camera renderCamera){
            float xDirection = (float)(Math.sin(yaw) * Math.cos(pitch));
            float yDirection = (float)Math.sin(pitch);
            float zDirection = (float)(Math.cos(yaw) * Math.cos(pitch));
            float[] directionToCamera = new float[]{xDirection, yDirection, zDirection};
            viewDirection[0] = directionToCamera[0] * (-1.0f);
            viewDirection[1] = directionToCamera[1] * (-1.0f);
            viewDirection[2] = directionToCamera[2] * (-1.0f);
            float[] eyePosition = new float[]{centerPosition[0] + directionToCamera[0] * radius,
                    centerPosition[1] + directionToCamera[1] * radius,
                    centerPosition[2] + directionToCamera[2] * radius,};

            renderCamera.pos = eyePosition;
            renderCamera.view = viewDirection;
            renderCamera.up = new float[]{0, 1, 0};
            renderCamera.resolution = resolution;
            renderCamera.fov = fov;
            renderCamera.apertureRadius = apertureRadius;
            renderCamera.focalDistance = focalDistance;
        }
    }
}
