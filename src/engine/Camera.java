package engine;

public class Camera {
    
    float[] pos;
    float speed;
    float tspeed;
    float[] lcorner = {-1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f};
    float[] rcorner = {1.0f, -1.0f, -1.0f,  1.0f, 1.0f, -1.0f};
    public Camera() {
        pos = new float[3];
        pos[0] = 0.0f;
        pos[1] = 0.0f;
        pos[2] = 0.0f;
        speed  = 0.05f;
        tspeed = 0.1f;
    }

    public void setPos(float x, float y, float z){
        pos[0] = x;
        pos[1] = y;
        pos[2] = z;
    }

    public float[] getPos(){return pos;}

    public void rotate(int dir){
        float m = dir * tspeed;
        lcorner[0] = lcorner[0] + m;
        lcorner[2] = lcorner[2] + m;
        lcorner[3] = lcorner[3] + m;
        lcorner[5] = lcorner[5] + m;
        rcorner[0] = rcorner[0] + m;
        rcorner[2] = rcorner[2] + m;
        rcorner[3] = rcorner[3] + m;
        rcorner[5] = rcorner[5] + m;

        // for(int i=0; i < lcorner.length; i++){
        //     lcorner[i] = lcorner[i] + m;
        // }
        // for(int i=0; i < rcorner.length; i++){
        //     rcorner[i] = rcorner[i] + m;
        // }
    }

    public void setRotation(int dir){

    }

    public float[] getL1(){
        float[] l1 = {lcorner[0], lcorner[1], lcorner[2]};
        return l1;
    }
    public float[] getL2(){
        float[] l2 = {lcorner[3], lcorner[4], lcorner[5]};
        return l2;
    }
    public float[] getR1(){
        float[] r1 = {rcorner[0], rcorner[1], rcorner[2]};
        return r1;
    }
    public float[] getR2(){
        float[] r2 = {rcorner[3], rcorner[4], rcorner[5]};
        return r2;
    }
    

    public float[][] getUniform(){
        float[] l1 = {lcorner[0], lcorner[1], lcorner[2]};
        float[] l2 = {lcorner[3], lcorner[4], lcorner[5]};
        float[] r1 = {rcorner[0], rcorner[1], rcorner[2]};
        float[] r2 = {rcorner[3], rcorner[4], rcorner[5]};

        float[][] out = {
            pos, 
            l1,
            l2,
            r1,
            r2
        };
        return out;
    }

}
