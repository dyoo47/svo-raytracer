public class NoiseSampler {

    private static float scale = 0.125f;

    public static int sample(int x, int y, int z){
        return (int) Math.round(SimplexNoise.noise(x*scale / 50f, y*scale / 50f, z*scale / 50f) * 3 % 2);
    }

    public static int sample(int x, int z){
        return (int) Math.round(SimplexNoise.noise(x*scale / 128f, z*scale / 128) * 128);
    }

    public static int sampleSphere(int x, int y, int z){
        int radius = 256;
        int sphereX = 512;
        int sphereY = 0;
        int sphereZ = 512;
        return (int)Math.round(Math.sqrt(Math.pow(sphereX - (x), 2) + Math.pow(sphereY - (y), 2) + Math.pow(sphereZ - (z), 2)) - radius);
    }
}
