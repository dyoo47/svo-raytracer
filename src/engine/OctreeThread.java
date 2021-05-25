public class OctreeThread implements Runnable {

    EfficientOctree octree;
    Thread thread;
    int maxLOD;
    String threadName;
    public OctreeThread(String name, EfficientOctree octree, int maxLOD){
        this.octree = octree;
        this.maxLOD = maxLOD;
        this.threadName = name;
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
        octree.constructOctree(maxLOD, 0);
    }
    
}
