public class WorldGenThread implements Runnable {
    Thread thread;
    String threadName;
    VoxelData vd;
    int[] offset;
    int[] origin;

    public WorldGenThread(String name, VoxelData voxelData, int[] offset, int[] origin){
        this.threadName = name;
        System.out.println("Creating thread " + name + "...");
        this.vd = voxelData;
        this.offset = offset;
        this.origin = origin;
    }

    public void start(){
        System.out.println("Starting " + threadName + ".");
        if(thread==null){
            thread = new Thread(this, threadName);
            thread.start();
        }
    }

    @Override
    public void run() {
        int xlim = (vd.width + vd.width * offset[0])/2;
        int ylim = (vd.height + vd.height * offset[1])/2;
        int zlim = (vd.depth + vd.depth * offset[2])/2;
        for(int i=(vd.width * offset[0])/2; i < xlim; i++){
            for(int j=(vd.height * offset[1])/2; j < ylim; j++){
                for(int k=(vd.depth * offset[2])/2; k < zlim; k++){
                    if(NoiseSampler.sample(origin[0] + i, origin[1] + j, origin[2] + k) > 0)
                        vd.fastSet(i, j, k, (byte) 1);
                    else 
                        vd.fastSet(i, j, k, (byte) 0);
                }
            }
        }
        System.out.println("Exiting " + threadName + ".");
    }
    
}
