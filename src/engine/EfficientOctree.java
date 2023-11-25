import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;


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
  static final int LEAF_SIZE = 3;
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
    buffer = ByteBuffer.allocateDirect(bufferSize);
    threads = new WorldGenThread[8];
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
  if a ray stops on a non-leaf node, we calculate the normal using the face it lands on.
  
  leaf
  0 :: value - 1 byte
  1 :: normal x
  2 :: normal y
  3 :: normal z
  */

  private int createNode(byte val){
    int pointer = memOffset;
    buffer.put(memOffset++, val);
    buffer.put(memOffset++, (byte)0);
    buffer.put(memOffset++, (byte)0);
    buffer.put(memOffset++, (byte)0);
    buffer.put(memOffset++, (byte)0);
    buffer.put(memOffset++, (byte)0);
    return pointer;
  }

  private int createLeafNode(byte val, short normal){
    int pointer = memOffset;
    buffer.put(memOffset++, val);
    buffer.put(memOffset++, (byte)(normal));
    buffer.put(memOffset++, (byte)(normal >> 8));
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
    createNode((byte) 1); //value shouldn't be read cuz root is never leaf node
    if(maxLOD <= 9){
      VoxelData vData = new VoxelData(size, size, size);
      for(int i=0; i < 8; i++){
        threads[i] = new WorldGenThread("wg-" + i, vData, Constants.CHILD_OFFSETS[i] , origin);
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

  }

  public byte getValue(int parentNode){
    return buffer.get(parentNode);
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
          threads[i] = new WorldGenThread("inner wg", voxelData, Constants.CHILD_OFFSETS[i], newOrigin);
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
      if(leaf) {
        if(cSize == 1){
          int normalX = 0;
          int normalY = 0;
          int normalZ = 0;
          for(int i = cPos[n][0]-1; i <= cPos[n][0]+1; i++){
            if(i < 0 || i >= voxelData.width) continue;
            for(int j = cPos[n][1]-1; j <= cPos[n][1]+1; j++){
              if(j < 0 || j >= voxelData.height) continue;
              for(int k = cPos[n][2]-1; k <= cPos[n][2]+1; k++){
                if(k < 0 || k >= voxelData.depth) continue;
                if(voxelData.get(i, j, k) == 0){
                  normalX += i - cPos[n][0];
                  normalY += j - cPos[n][1];
                  normalZ += k - cPos[n][2];
                }
              }
            }
          }
          normalX = normalX / 2 + 5;
          normalY = normalY / 2 + 5;
          normalZ = normalZ / 2 + 5;
          short packed = (short)(normalX + normalY * 10 + normalZ * 100);
          //System.out.println(normalX + ", " + normalY + ", " + normalZ + " => " + packed);
          children[n] = createLeafNode(value, packed);
        }else{ //TODO: Generalized algorithm for voxels of size N>1 needs work.
          int normalX = 0;
          int normalY = 0;
          int normalZ = 0;
          for(int i = cPos[n][0]-1; i <= cPos[n][0]+cSize; i++){
            if(i < 0 || i >= voxelData.width || i >= cPos[n][0] && i <= cPos[n][0]+cSize-1) continue;
            for(int j = cPos[n][1]-1; j <= cPos[n][1]+cSize; j++){
              if(j < 0 || j >= voxelData.height || j >= cPos[n][1] && j <= cPos[n][1]+cSize-1) continue;
              for(int k = cPos[n][2]-1; k <= cPos[n][2]+cSize; k++){
                if(k < 0 || k >= voxelData.depth || k >= cPos[n][2] && k <= cPos[n][2]+cSize-1) continue;
                if(voxelData.get(i, j, k) == 0){
                  normalX += Math.copySign(1, i-cPos[n][0]);
                  normalY += Math.copySign(1, j-cPos[n][1]);
                  normalZ += Math.copySign(1, k-cPos[n][2]);
                }
              }
            }
          }
          float maxParam = 2*(cSize+2)*(cSize+2); //calculating max value of a single normal parameter
          float fnx = normalX / maxParam;
          float fny = normalY / maxParam;
          float fnz = normalZ / maxParam;
          float fnmax = Math.max(Math.abs(fnx), Math.max(Math.abs(fny), Math.abs(fnz)));
          //instead of dividing by fnmax we can multiply fn by a constant then subtract so fnmax = 1.
          normalX = (int)(fnx/fnmax * 9) / 2 + 5;
          normalY = (int)(fny/fnmax * 9) / 2 + 5;
          normalZ = (int)(fnz/fnmax * 9) / 2 + 5;
          short packed = (short)(normalX + normalY * 10 + normalZ * 100);
          children[n] = createLeafNode(value, packed);
        }
      }
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
    return buffer;
  }

  public void writeBufferToFile(String fileName){
    try{
      File outfile = new File(fileName);
      ByteBuffer buf = this.getByteBuffer();
      FileOutputStream fs = new FileOutputStream(outfile, false);
      ByteBuffer header = ByteBuffer.allocate(4);
      header.putInt(memOffset);
      System.out.println(header.getInt(0));
      
      fs.write(header.array());
      fs.getChannel().write(buf);
      fs.getChannel().close();
      fs.close();
    }catch(IOException e){
      System.out.println("Error while writing buffer to " + fileName + ": ");
      e.printStackTrace();
    }
  }

  public void readBufferFromFile(String fileName){
    try{
      SeekableByteChannel ch = Files.newByteChannel(Paths.get(fileName), StandardOpenOption.READ);
      ByteBuffer header = ByteBuffer.allocate(4);

      ch.read(header);
      ch.read(buffer);
      buffer.flip();

      this.memOffset = header.getInt(0);
      ch.close();
    }catch(Exception e){
      System.out.println("Error while reading file " + fileName + ": ");
      e.printStackTrace();
    }
  }

  public void editLeafNodeValue(int pointer, byte val){
    buffer.put(pointer, val);
  }

}
