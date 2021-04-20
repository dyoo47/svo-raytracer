import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import org.lwjgl.BufferUtils;

public class Octree {

    byte[] mem;
    ByteBuffer buffer;
    int memOffset = 0;
    int bufferSize = 0;

    static int NODE_SIZE = 8;
    // static byte[][] childOffsets = {
    //         {0, 0, 0},
    //         {0, 0, 1},
    //         {0, 1, 0},
    //         {0, 1, 1},
    //         {1, 0, 0},
    //         {1, 0, 1},
    //         {1, 1, 0},
    //         {1, 1, 1}
    // };
    static byte[][] childOffsets = {
                {0, 0, 0},
                {1, 0, 0},
                {0, 1, 0},
                {1, 1, 0},
                {0, 0, 1},
                {1, 0, 1},
                {0, 1, 1},
                {1, 1, 1}
    };

    public Octree(int memSizeKB, byte size, VoxelData voxelData){
        bufferSize = memSizeKB * 1024;
        buffer = ByteBuffer.allocate(bufferSize);
        mem = buffer.array();
        createNode(size, (byte) 1, new byte[]{0, 0, 0}, false);
    }

    /*
    NODE STRUCTURE-----------------------
    0 :: size - 1 byte
    1 :: value - 1 byte
    2 :: x - position - 3 bytes
    3 :: y
    4 :: z
    5 :: child pointer - 2 bytes    0 == null pointer as 0 is always taken by root node
    6 ::
    7 :: is leaf boolean - 1 byte
     */

    public void logBuffer(int length){
        int t = 0;
        System.out.println("BEGIN BUFFER******************");
        for(int i = 0; i < length; i++){
            if(t == 0) System.out.println("BEGIN NODE---");

            if(t == 5) {
                System.out.println("CP: " + buffer.getShort(i));
            }else if(t == 6){
                //continue;
            }else{
                System.out.println(t + "-" + i + "::" + mem[i]);
            }

            t++;
            if(t == 8){
                System.out.println("END NODE-----");
                t = 0;
            }
        }
        System.out.println("END BUFFER********************");
    }

    public void clear(){
        buffer.clear();
    }

    public ByteBuffer getByteBuffer(){
        ByteBuffer out = BufferUtils.createByteBuffer(memOffset);
        for(int i=0; i < memOffset; i++){
            out.put(i, buffer.get(i));
        }
        return out;
    }

    public IntBuffer getIntBuffer(){
        IntBuffer out = BufferUtils.createIntBuffer(memOffset);
        int t = 0;
        for(int i=0; i < memOffset; i++){
            if(t == 5){
                out.put(i, buffer.getShort(i));
            }else if(t == 6){
                out.put(i, 0);
            }else{
                out.put(i, buffer.get(i));
            }
            t++;
            if(t == 8) t=0;
        }
        // for(int i=0; i < memOffset; i++){
        //     out.put(i, buffer.get(i));
        // }
        return out;
    }

    private int createNode(byte size, byte val, byte[] pos, boolean isLeaf){
        //returns pointer to new node in mem buffer
        if(memOffset + NODE_SIZE > mem.length){
            System.out.println("Buffer Overflow!");
            return -2;
        }
        int pointer = memOffset;
        mem[memOffset++] = size;
        mem[memOffset++] = val;
        mem[memOffset++] = pos[0];
        mem[memOffset++] = pos[1];
        mem[memOffset++] = pos[2];
        mem[memOffset++] = 0;
        mem[memOffset++] = 0;
        if(isLeaf) mem[memOffset++] = 1;
        else mem[memOffset++] = 0;
        return pointer;
    }

    private void setChildPointer(int parentNode, short childPointer){
        //mem[parentNode + 5] = (byte) (childPointer & 0xFF);
        //mem[parentNode + 6] = (byte) ((childPointer >> 8) & 0xFF);
        buffer.putShort(parentNode + 5, childPointer);
        //System.out.println("AT POINT: " + parentNode);
    }


    private int getChildPointer(int parentNode, int child){
        short ret = buffer.getShort(parentNode + 5);
        return ret + (child * NODE_SIZE);
    }

    public byte getSize(int parentNode){
        return mem[parentNode];
    }

    public byte getValue(int parentNode){
        return mem[parentNode + 1];
    }

    public void setValue(int parentNode, byte value){
        mem[parentNode + 1] = value;
    }

    public byte[] getPos(int parentNode){
        return new byte[]{mem[parentNode + 2], mem[parentNode + 3], mem[parentNode + 4]};
    }

    private boolean isLeaf(int node){
        return mem[node + 7] == 1;
    }

    public void constructOctree(VoxelData data, int maxLOD, int curLOD, int parentNode){

        byte pSize = getSize(parentNode);
        byte[] pPos = new byte[]{mem[parentNode + 2], mem[parentNode + 3], mem[parentNode + 4]};
        int firstPointer = -1;
        for(int n = 0; n < 8; n++){

            byte cSize = (byte)(pSize / 2);
            byte[] cPos = new byte[]{
                    (byte)(pPos[0] + childOffsets[n][0] * cSize),
                    (byte)(pPos[1] + childOffsets[n][1] * cSize),
                    (byte)(pPos[2] + childOffsets[n][2] * cSize)
            };
            byte first = data.get(cPos[0], cPos[1], cPos[2]);
            byte value = first;
            boolean isLeaf = true;
            for(int i = cPos[0]; i < cPos[0] + cSize; i++){
                for(int j = cPos[1]; j < cPos[1] + cSize; j++){
                    for(int k = cPos[2]; k < cPos[2] + cSize; k++){
                        if(data.get(i, j, k) != 0){
                            value = data.get(i, j, k);
                        }
                        if(data.get(i, j, k) != first){
                            if(first == 0) first = data.get(i, j, k);
                            value = first;
                            isLeaf = false;
                            break;
                        }
                        // if(data.get(i, j, k) != 0){
                        //     first = data.get(i, j, k);
                        //     valid = false;
                        //     break;
                        // }
                    }
                    if(!isLeaf) break;
                }
                if(!isLeaf) break;
            }
            //child nodes must be made consecutively - allows for only one pointer per node
            //if(cSize == 1 || maxLOD == curLOD) valid = true;
            if(n == 0){
                firstPointer = createNode(cSize, value, cPos, isLeaf);
            }else{
                createNode(cSize, value, cPos, isLeaf);
            }
        }
        if(firstPointer != -1)
            setChildPointer(parentNode, (short)firstPointer);
        for(int n = 0; n < 8; n++){
            int pointer = firstPointer + (n * NODE_SIZE);
            if(!isLeaf(pointer) && curLOD < maxLOD){ //this line
                constructOctree(data, maxLOD, curLOD + 1, pointer);
            }
        }
    }

    public ArrayList<Integer> getChildrenAtLOD(int maxLOD, int curLOD, int origin){
        ArrayList<Integer> target = new ArrayList<>();
        if(isLeaf(origin) || maxLOD == curLOD){
            if(getValue(origin) == 1){
                target.add(origin);
            }
        }else{
            for(int n = 0; n < 8; n++){
                target.addAll(getChildrenAtLOD(maxLOD, curLOD + 1, getChildPointer(origin, n)));
            }
        }
        return target;
    }
}