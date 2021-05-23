public class NoiseSampler {

    private static float scale = 0.125f;

    public static int sample(int x, int y, int z){
        return (int) Math.round(SimplexNoise.noise(x*scale / 50f, y*scale / 50f, z*scale / 50f));
    }

    public static int sample(int x, int z){
        return (int) Math.round(SimplexNoise.noise(x*scale / 128f, z*scale / 128) * 128);
    }
}
