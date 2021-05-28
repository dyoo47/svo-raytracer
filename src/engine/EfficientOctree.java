import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;

public class EfficientOctree {
  byte[] mem;
  ByteBuffer buffer;
  int memOffset = 0;
  int bufferSize = 0;
  WorldGenThread[] threads;
  int[] origin;
  int size;
  int splitLOD;

  static final int NODE_SIZE = 6;
  static final int LEAF_SIZE = 1;
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

  public EfficientOctree(int memSizeKB, int size, int[] origin){
    this.size = size;
    this.origin = origin;
    bufferSize = memSizeKB * 1024;
    buffer = ByteBuffer.allocate(bufferSize); //TODO: change to BufferUtils.createByteBuffer(bufferSize);
    mem = buffer.array();
    //origin = new int[]{0, -512, 0};
    createNode((byte) 1);
    threads = new WorldGenThread[8];
    // for(int i=0; i < 8; i++){
    //   threads[i] = new WorldGenThread("wg-" + i, null, Constants.childOffsets[i], origin);
    // }
    //this.voxelData = voxelData;
  }

  public EfficientOctree(int memSizeKB, String svoFile){
    bufferSize = memSizeKB * 1024;
    readBufferFromFile(svoFile);
  }

  /*
  NODE STRUCTURE
  branch
  0 :: value - 1 byte
  1 :: child pointer - 4 bytes
  2 ::
  3 ::
  4 ::
  5 :: leaf mask

  leaf
  0 :: value - 1 byte
  */

  private int createNode(byte val){
    int pointer = memOffset;
    mem[memOffset++] = val;
    mem[memOffset++] = 0;
    mem[memOffset++] = 0;
    mem[memOffset++] = 0;
    mem[memOffset++] = 0;
    mem[memOffset++] = 0;
    return pointer;
  }

  private int createLeafNode(byte val){
    int pointer = memOffset;
    mem[memOffset++] = val;
    return pointer;
  }

  private void setChildPointer(int parentNode, int childPointer){
    buffer.putInt(parentNode + 1, childPointer);
  }

  private void setLeafMask(int parentNode, byte leafMask){
    buffer.put(parentNode + 5, leafMask);
  }

  public void constructOctree(int maxLOD, int rootPointer){
    int maxSize = 1 << maxLOD;
    int[] rootPos = {0, 0, 0};
    createNode((byte) 0); //value shouldn't be read cuz root is never leaf node
    if(maxLOD <= 9){
      VoxelData vData = new VoxelData(1024, 1024, 1024);
      for(int i=0; i < 8; i++){
        threads[i] = new WorldGenThread("wg-" + i, vData, Constants.childOffsets[i] , origin);
        threads[i].start();
      }
      int i = 0;
      while(i < 8){
        i = 0;
        for(WorldGenThread t : threads){
          if(!t.thread.isAlive()) i++;
        }
      }
      constructOctree(maxSize, 0, rootPos, rootPointer, vData, false);
    }else{
      splitLOD = maxLOD - 9;
      maxSize = 512;
      constructOctree(maxSize, 0, rootPos, rootPointer, null, false);
    }
    //constructOctree(maxSize, 0, rootPos, rootPointer, null, false);
    //vData = null;
  }

  public byte getValue(int parentNode){
    return mem[parentNode];
  }

  private void constructOctree(int maxSize, int curLOD, int[] pPos, int parentPointer, VoxelData voxelData, boolean split){

    int cSize = maxSize >> curLOD;
    if(cSize == 0) return;

    int[] children = {0, 0, 0, 0, 0, 0, 0, 0};
    int[][] cPos = new int[8][3];
    for(int n = 0; n < 8; n++){
      cPos[n][0] = pPos[0] + childOffsets[n][0] * cSize;
      cPos[n][1] = pPos[1] + childOffsets[n][1] * cSize;
      cPos[n][2] = pPos[2] + childOffsets[n][2] * cSize;
    }
    if(voxelData == null){

      if(curLOD == splitLOD && !split){
        int[] newOrigin = {
          origin[0] + pPos[0]*2,
          origin[1] + pPos[1]*2,
          origin[2] + pPos[2]*2
        };
        pPos[0] = 0;
        pPos[1] = 0;
        pPos[2] = 0;
        voxelData = new VoxelData(1024, 1024, 1024);
        
        for(int i=0; i < 8; i++){
          threads[i] = new WorldGenThread("inner wg", voxelData, Constants.childOffsets[i], newOrigin);
          threads[i].start();
        }
        int i = 0;
        while(i < 8){
          i = 0;
          for(WorldGenThread t : threads){
            if(!t.thread.isAlive()) i++;
          }
        }
        constructOctree(maxSize, 0, pPos, parentPointer, voxelData, true);
        return;
      }else{
        for(int i = 0; i < 8; i++){
          children[i] = createNode((byte) 1);
        }
        setChildPointer(parentPointer, children[0]);
        for(int i = 0; i < 8; i++){
          constructOctree(maxSize, curLOD + 1, cPos[i], children[i], voxelData, split);
        }
        return;
      }
    }
    byte leafMask = 0;
    for(int n = 0; n < 8; n++){
      byte first = voxelData.get(cPos[n][0], cPos[n][1], cPos[n][2]);
      byte value = first;
      boolean leaf = true;
      for(int i = cPos[n][0]; i < cPos[n][0] + cSize; i++){
        for(int j = cPos[n][1]; j < cPos[n][1] + cSize; j++){
          for(int k = cPos[n][2]; k < cPos[n][2] + cSize; k++){
            byte sample = voxelData.get(i, j, k);
            if(sample != 0){
              value = sample;
            }
            if(sample != first){
              if(first == 0) first = sample;
              value = first;
              leaf = false;
              break;
            }
          }
          if(!leaf) break;
        }
        if(!leaf) break;
      }
      if(leaf) children[n] = createLeafNode(value);
      else children[n] = createNode(value);
      if(leaf) leafMask |= (0x01 << n);
    }
    setChildPointer(parentPointer, children[0]);
    setLeafMask(parentPointer, leafMask);
    for(int n = 0; n < 8; n++){
      if(getValue(children[n]) != 0 && (leafMask & (0x01 << n)) == 0){
        constructOctree(maxSize, curLOD + 1, cPos[n], children[n], voxelData, split);
      }
    }
  }
  
  public ByteBuffer getByteBuffer(){
    int limit = memOffset;
    ByteBuffer out = BufferUtils.createByteBuffer(limit);
    for(int i=0; i < limit; i++){
      out.put(i, buffer.get(i));
    }
    return out;
  }

  public void writeBufferToFile(String fileName){
    try{
      File outfile = new File(fileName);
      ByteBuffer buf = this.getByteBuffer();
      FileOutputStream fs = new FileOutputStream(outfile, false);
      //buf.flip();
      fs.getChannel().write(buf);
      fs.getChannel().close();
      fs.close();
    }catch(IOException e){
      System.out.println("Error while writing buffer to " + fileName + ": " + e.getMessage());
    }
  }

  public void readBufferFromFile(String fileName){
    try{
      File infile = new File(fileName);
      ByteBuffer buf = ByteBuffer.allocate(bufferSize);
      FileInputStream fs = new FileInputStream(infile);
      int b;
      this.memOffset = 0;
      while((b=fs.read())!=-1){
        memOffset++;
        buf.put((byte)b);
      }
      this.buffer = buf;
      this.mem = buf.array();
      fs.close();
    }catch(IOException e){
      System.out.println("Error while reading buffer from " + fileName + ": " + e.getMessage());
    }
  }
}
