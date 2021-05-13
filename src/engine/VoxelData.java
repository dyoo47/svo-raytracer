public class VoxelData {

    byte[] data;
    private final int width;
    private final int height;
    private final int depth;

    public VoxelData(int width, int height, int depth){
        data = new byte[width * height * depth];
        this.width = width;
        this.height = height;
        this.depth = depth;
        for(int i=0; i < width; i++){
            for(int j=0; j < height; j++){
                for(int k=0; k < depth; k++){
                    set(i, j, k, (byte) 0);//sampleSphere(i, j, k);
                }
            }
        }
    }

    public byte get(int x, int y, int z){
        if(x < 0 || x >= width) return 0;
        if(y < 0 || y >= height) return 0;
        if(z < 0 || z >= depth) return 0;
        return data[x + z * width + y * (width * height)];
    }

    public void set (int x, int y, int z, byte voxel) {
        if (x < 0 || x >= width) return;
        if (y < 0 || y >= height) return;
        if (z < 0 || z >= depth) return;
        fastSet(x, y, z, voxel);
    }

    public void fastSet(int x, int y, int z, byte voxel){
        data[x + z * width + y * (width * height)] = voxel;
    }

    public void sample(int x, int y, int z){
        for(int i = 0; i < width; i++){
            for(int j = 0; j < height; j++){
                for(int k = 0; k < depth; k++){
                    int sample = (int) Math.round(SimplexNoise.noise((i + x) / 50f, (j + y) / 50f, (k + z) / 50f));
                    if(sample > 0){
                        fastSet(i, j, k, (byte) 1);
                    }else{
                        fastSet(i, j, k, (byte) 0);
                    }
                }
            }
        }
    }

    public void sampleMod(int x, int y, int z){
        for(int i = 0; i < width; i++){
            for(int j = 0; j < height; j++){
                for(int k = 0; k < depth; k++){
                    int sample = (int) Math.round(Math.abs(SimplexNoise.noise((i + x) / 50f, (j + y) / 50f, (k + z) / 50f) * 3 % 2));
                    if(sample == 0){
                        fastSet(i, j, k, (byte) 1);
                    }else{
                        fastSet(i, j, k, (byte) 0);
                    }
                }
            }
        }
    }

    public void sampleTest(int x, int y, int z){
        for(int i = 0; i < width; i++){
            for(int j = 0; j < height; j++){
                for(int k = 0; k < depth; k++){
                    if(k % 2 == 0){
                        fastSet(i, j, k, (byte)1);
                    }else{
                        fastSet(i, j, k, (byte)0);
                    }

                }
            }
        }
    }

    public void sampleCaves(int x, int y, int z){
        for(int i = 0; i < width; i++){
            for(int j = 0; j < height; j++){
                for(int k = 0; k < depth; k++){
                    int sample = (int) Math.round(SimplexNoise.noise((i + x) / 50f, (j + y) / 50f, (k + z) / 50f));
                    if(sample == 0){
                        fastSet(i, j, k, (byte) 1);
                    }else{
                        fastSet(i, j, k, (byte) 0);
                    }
                }
            }
        }
    }

    public void sample2D(int x, int y, int z){
        for(int i = 0; i < width; i++){
            for(int j = 0; j < height; j++){
                for(int k = 0; k < depth; k++){
                    int sample = (int) Math.round(((SimplexNoise.noise((i + x) / 50f, (k + z) / 50f) + 1) * 4) + ((SimplexNoise.noise((i + x) / 200f, (k + z) / 200f) + 1) * 32));
                    if(j + y <= sample){
                        fastSet(i, j, k, (byte) 1);
                    }else{
                        fastSet(i, j, k, (byte) 0);
                    }
                }
            }
        }
    }

    public void sampleRidges(int x, int y, int z){
        for(int i = 0; i < width; i++){
            for(int j = 0; j < height; j++){
                for(int k = 0; k < depth; k++){
                    int sample = (int) Math.round(((SimplexNoise.noise((i + x) / 50f, (k + z) / 50f) + 1) * 4) + Math.pow((1 - Math.abs(SimplexNoise.noise((i + x) / 200f, (k + z) / 200f))), 3) * 32);
                    if(j + y <= sample){
                        fastSet(i, j, k, (byte) 1);
                    }else{
                        fastSet(i, j, k, (byte) 0);
                    }
                }
            }
        }
    }

    public void sampleSphere(int x, int y, int z){
        System.out.println("sampling sphere");
        int sphereX = 64;
        int sphereY = 64;
        int sphereZ = 64;
        int sphereRadius = 64;

        for(int i = 0; i < width; i++){
            for(int j = 0; j < height; j++){
                for(int k = 0; k < depth; k++){
                    int sample = (int) Math.round(Math.sqrt(Math.pow(sphereX - (x + i), 2) + Math.pow(sphereY - (y + j), 2) + Math.pow(sphereZ - (z + k), 2)) - sphereRadius);
                    if(sample < 0){
                        fastSet(i, j, k, (byte) 1);
                    }else{
                        fastSet(i, j, k, (byte) 0);
                    }
                }
            }
        }
    }

    public void sampleFull(int x, int y, int z){
        for(int i = 0; i < width; i++){
            for(int j = 0; j < height; j++){
                for(int k = 0; k < depth; k++){
                    fastSet(i, j, k, (byte) 1);
                }
            }
        }
    }
}
