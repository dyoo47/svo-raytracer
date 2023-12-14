package src.engine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.BufferUtils;

public class Octree {
  ByteBuffer buffer;
  int memOffset = 0;
  int bufferSize = 0;

  static final int NODE_SIZE = 6;
  static final int LEAF_SIZE = 3;
  static final int CHUNK_SIZE = 1024;
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

  class Chunk {
    int[] origin;
    int pointer;
    public Chunk(int[] origin, int pointer){
      this.origin = origin;
      this.pointer = pointer;
    }
  }

  public Octree(int memSizeKB){
    bufferSize = memSizeKB * 1024;
    buffer = ByteBuffer.allocateDirect(bufferSize);
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

  public void createDummyHead(){
    //TODO: Add error handling
    createNode((byte)1);
  }

  public byte getValue(int parentNode){
    return buffer.get(parentNode);
  }

  public void setValue(int parentNode, byte value){
    buffer.put(parentNode, value);
  }

  private byte getVoxel(ByteBuffer voxelData, int x, int y, int z){
    return voxelData.get(x | (y << 10) | (z << 20));
  }

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

  private void setChildPointer(int parentPointer, int childPointer){
    buffer.putInt(parentPointer + 1, childPointer - parentPointer);
  }

  private void setLeafMask(int parentPointer, byte leafMask){
    buffer.put(parentPointer + 5, leafMask);
  }

  private byte getLeafMask(int parentPointer){
    return buffer.get(parentPointer + 5);
  }

  public void printBufferToFile(String fileName){
    try{

      PrintWriter pw = new PrintWriter(new File(fileName));
      for(int i=0; i<memOffset; i++){
        pw.println(i + ": " + buffer.get(i));
      }
      pw.flush();
      pw.close();
    }catch(Exception e){
      e.printStackTrace();
    }
  }

  public void constructCompleteOctree(Renderer.Shader chunkGenShader, int texture){
    int[] rootPos = {0, -1024, 0};

    // construct root
    createNode((byte) 1);

    // create empty levels up to chunk size
    int chunkLevel = 1;

    // should calculate this from chunk level
    int worldSize = 2048;

    // generate octrees for each chunk
    ArrayList<Chunk> chunks = new ArrayList<Chunk>();
    fillEmptyChildren(0, chunkLevel, rootPos, chunks);
    int ind = 0;
    int half = worldSize/2;
    int[] playerPos = {rootPos[0] + half, rootPos[1] + half, rootPos[2] + half};
    System.out.println("Simulated Player Pos: " + playerPos[0] + ", " + playerPos[1] + ", " + playerPos[2]);
    for(Chunk chunk : chunks){
      ind++;
      int dist = Math.max(
        Math.abs(playerPos[0] - chunk.origin[0]), Math.max(Math.abs(playerPos[1] - chunk.origin[1]),
        Math.abs(playerPos[2] - chunk.origin[2])));
      
      int maxLOD = 9;

      if(dist >= 2048){
        maxLOD = 8;
      }
      if(dist >= 3072){
        maxLOD = 6;
      }
      if(dist >= 4096){
        maxLOD = 5;
      }

      System.out.println("Initializing chunk [" + chunk.origin[0] + ", " + chunk.origin[1] 
        + ", " + chunk.origin[2] + "]: " + chunk.pointer + ":" + dist + ":" + maxLOD);
      System.out.println(ind + "/" + chunks.size());

      double startTime = System.currentTimeMillis();

      Renderer renderer = Renderer.getInstance();
      int groupSize = CHUNK_SIZE/8;

      renderer.useProgram(chunkGenShader);
      renderer.setUniformInteger(1, chunk.origin[0]);
      renderer.setUniformInteger(2, chunk.origin[1]);
      renderer.setUniformInteger(3, chunk.origin[2]);

      renderer.printGLErrors();
      renderer.dispatchCompute(chunkGenShader, groupSize, groupSize, groupSize);

      ByteBuffer voxelBuffer = BufferUtils.createByteBuffer(CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE);

      renderer.printGLErrors();
      renderer.get3DTextureData(texture, voxelBuffer);

      double endTime = System.currentTimeMillis() - startTime;
      System.out.println("Voxel generation elapsed time: " + endTime / 1000 + "s");

      startTime = System.currentTimeMillis();

      int[] startPos = {0, 0, 0};
      int cSize = CHUNK_SIZE / 2;
      int[] children = {0, 0, 0, 0, 0, 0, 0, 0};
      OctreeThread[] threads = new OctreeThread[8];
      int[][] cPos = new int[8][3];
      for(int n = 0; n < 8; n++){
        cPos[n][0] = startPos[0] + childOffsets[n][0] * cSize;
        cPos[n][1] = startPos[1] + childOffsets[n][1] * cSize;
        cPos[n][2] = startPos[2] + childOffsets[n][2] * cSize;
      }
      for(int i=0; i < 8; i++){
        threads[i] = new OctreeThread(cPos[i], voxelBuffer);
        threads[i].start();
      }
      int finishedThreads = 0;
      while(finishedThreads < 8){
        finishedThreads = 0;
        for(OctreeThread thread : threads){
          if(!thread.isAlive()) finishedThreads++;
        }
      }

      for(int i=0; i < 8; i++){
        children[i] = createNode((byte) 1);
      }
      setChildPointer(chunk.pointer, children[0]);

      for(int i=0; i < 8; i++){

        OctreeThread thread = threads[i];
        ByteBuffer childBuffer = thread.octree.buffer;
        int childOffset = thread.octree.memOffset;
        byte headLeafMask = thread.octree.getLeafMask(0);

        // Set child pointer to start of new buffer, copy over leaf mask from dummy head
        setChildPointer(children[i], memOffset);
        setLeafMask(children[i], headLeafMask);

        // Copy over buffer minus dummy head
        childBuffer.position(NODE_SIZE).limit(childOffset);
        buffer.position(memOffset).put(childBuffer);
        memOffset += childOffset;

      }
      

      endTime = System.currentTimeMillis() - startTime;
      System.out.println("Octree generation elapsed time: " + endTime / 1000 + "s");
      System.out.println("memoffset: " + memOffset);
      System.out.println("usage: " + (float)memOffset / 1024 / 1024 + "MB");
    }
  }


  private void fillEmptyChildren(int parentPointer, int levels, int[] pPos, List<Chunk> chunks){
    if(levels == 0){
      chunks.add(new Chunk(pPos, parentPointer));
      return;
    }

    int cSize = CHUNK_SIZE << (levels - 1);
    int[][] cPos = new int[8][3];
    for(int n = 0; n < 8; n++){
      cPos[n][0] = pPos[0] + childOffsets[n][0] * cSize;
      cPos[n][1] = pPos[1] + childOffsets[n][1] * cSize;
      cPos[n][2] = pPos[2] + childOffsets[n][2] * cSize;
    }
    int[] children = new int[8];
    for(int i=0; i<8; i++){
      children[i] = createNode((byte) 1);
    }
    for(int i=0; i < 8; i++){
      fillEmptyChildren(children[i], levels - 1, cPos[i], chunks);
    }
    setChildPointer(parentPointer, children[0]);
  }
  
  public void constructOctree(int size, int curLOD, int maxLOD, int[] pPos, int parentPointer, ByteBuffer voxelData){

    int cSize = size / 2;
    if(cSize == 0 || curLOD == maxLOD) return;

    int[] children = {0, 0, 0, 0, 0, 0, 0, 0};
    int[][] cPos = new int[8][3];
    for(int n = 0; n < 8; n++){
      cPos[n][0] = pPos[0] + childOffsets[n][0] * cSize;
      cPos[n][1] = pPos[1] + childOffsets[n][1] * cSize;
      cPos[n][2] = pPos[2] + childOffsets[n][2] * cSize;
    }
    byte leafMask = 0;
    for(int n = 0; n < 8; n++){
      byte first = getVoxel(voxelData, cPos[n][0], cPos[n][1], cPos[n][2]);
      byte value = first;
      boolean leaf = true;
      //if next LOD is maxLOD, then we can assume all children are leaves.
      if(curLOD + 1 != maxLOD){
        for(int i = cPos[n][2]; i < cPos[n][2] + cSize; i++){
          for(int j = cPos[n][1]; j < cPos[n][1] + cSize; j++){
            for(int k = cPos[n][0]; k < cPos[n][0] + cSize; k++){
              byte sample = getVoxel(voxelData, k, j, i);
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
      }
      if(leaf) {
        if(cSize == 1){
          int normalX = 0;
          int normalY = 0;
          int normalZ = 0;
          for(int i = cPos[n][0]-1; i <= cPos[n][0]+1; i++){
            if(i < 0 || i >= CHUNK_SIZE) continue;
            for(int j = cPos[n][1]-1; j <= cPos[n][1]+1; j++){
              if(j < 0 || j >= CHUNK_SIZE) continue;
              for(int k = cPos[n][2]-1; k <= cPos[n][2]+1; k++){
                if(k < 0 || k >= CHUNK_SIZE) continue;
                if(getVoxel(voxelData, i, j, k) == 0){
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
          children[n] = createLeafNode(value, packed);
        }else{ //TODO: Generalized algorithm for voxels of size N>1 needs work.
          int normalX = 0;
          int normalY = 0;
          int normalZ = 0;
          for(int i = cPos[n][2]-1; i <= cPos[n][2]+cSize; i++){
            if(i < 0 || i >= CHUNK_SIZE || i >= cPos[n][0] && i <= cPos[n][0]+cSize-1) continue;
            for(int j = cPos[n][1]-1; j <= cPos[n][1]+cSize; j++){
              if(j < 0 || j >= CHUNK_SIZE || j >= cPos[n][1] && j <= cPos[n][1]+cSize-1) continue;
              for(int k = cPos[n][0]-1; k <= cPos[n][0]+cSize; k++){
                if(k < 0 || k >= CHUNK_SIZE || k >= cPos[n][2] && k <= cPos[n][2]+cSize-1) continue;
                if(getVoxel(voxelData, k, j, i) == 0){
                  normalX += Math.copySign(1, k-cPos[n][0]);
                  normalY += Math.copySign(1, j-cPos[n][1]);
                  normalZ += Math.copySign(1, i-cPos[n][2]);
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
        constructOctree(cSize, curLOD + 1, maxLOD, cPos[n], children[n], voxelData);
      }
    }
  }
  
  public ByteBuffer getByteBuffer(){
    return buffer;
  }

  public void writeBufferToFile(String fileName){
    buffer.position(0);
    try{
      File outfile = new File(Constants.MAP_DIR + fileName);
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
      SeekableByteChannel ch = Files.newByteChannel(Paths.get(Constants.MAP_DIR + fileName), StandardOpenOption.READ);
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
