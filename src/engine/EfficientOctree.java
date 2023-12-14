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

public class EfficientOctree {
  byte[] mem;
  ByteBuffer buffer;
  int memOffset = 0;
  int bufferSize = 0;
  int worldSize;
  WorldGenThread[] threads;
  int[] origin;

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

  public EfficientOctree(int memSizeKB, int worldSize, int[] origin){
    this.worldSize = worldSize;
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

  public void createDummyHead(){
    //TODO: Add error handling
    createNode((byte)1);
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

  public void constructDebugOctree(Renderer.Shader chunkGenShader, int texture){
    int[] rootPos = {0, -1024, 0};

    //construct root
    createNode((byte) 1);

    // create empty levels up to chunk size
    int chunkLevel = 1;

    // generate octrees for each chunk
    ArrayList<Chunk> chunks = new ArrayList<Chunk>();
    fillEmptyChildren(0, chunkLevel, rootPos, chunks);
    int ind = 0;
    int half = worldSize/2;
    int[] playerPos = {rootPos[0] + half, rootPos[1] + half, rootPos[2] + half};
    System.out.println("Simulated Player Pos: " + playerPos[0] + ", " + playerPos[1] + ", " + playerPos[2]);
    for(Chunk chunk : chunks){
      // setLeafMask(chunk.pointer, (byte) 255);
      // for(int i=0; i < 8; i++){
      //   int childPointer = 0;
      //   if(i == 0 || i == 7) childPointer = createLeafNode((byte) 0, (short) 0);
      //   else childPointer = createLeafNode((byte) 1, (short) 0);
      //   if(i == 0) setChildPointer(chunk.pointer, childPointer);
      // }
      // setLeafMask(chunk.pointer, (byte) 255);
      // for(int i=0; i < 8; i++){
      //   int childPointer = 0;
      //   if(i%2 == 0) childPointer = createLeafNode((byte) 1, (short) 0);
      //   else childPointer = createLeafNode((byte) 0, (short) 0);
      //   if(i == 0) setChildPointer(chunk.pointer, childPointer);
      // }
      // chunk.origin = new int[]{0, 0, 0};
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
      // System.out.println("Dispatched compute");

      ByteBuffer voxelBuffer = BufferUtils.createByteBuffer(CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE);
      // System.out.println("Allocated buffer");

      renderer.printGLErrors();
      renderer.get3DTextureData(texture, voxelBuffer);
      // System.out.println("Retrieved 3d texture data");

      double endTime = System.currentTimeMillis() - startTime;
      System.out.println("Voxel generation elapsed time: " + endTime / 1000 + "s");

      startTime = System.currentTimeMillis();
      int[] startPos = {0, 0, 0};

      // setLeafMask(chunk.pointer, (byte)255);
      // setChildPointer(chunk.pointer, memOffset);
      // for(int i=0; i < 8; i++){
      //   ByteBuffer childBuffer = BufferUtils.createByteBuffer(3);
      //   byte value = 0;
      //   if(i%2 == 0) value = 1;
      //   childBuffer.put(value);
      //   childBuffer.put((byte)(0));
      //   childBuffer.put((byte)(0));
      //   childBuffer.position(0);
      //   childBuffer.limit(3);
      //   buffer.position(memOffset).put(childBuffer);
      //   memOffset += 3;
      // }

      // constructOctree(CHUNK_SIZE, 0, maxLOD, startPos, chunk.pointer, voxelBuffer);
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

      //Problem is children are not contiguous nodes.
      for(int i=0; i < 8; i++){
        children[i] = createNode((byte) 1);
      }
      setChildPointer(chunk.pointer, children[0]);

      for(int i=0; i < 8; i++){ //TODO: Change this back to 8

        OctreeThread thread = threads[i];
        ByteBuffer childBuffer = thread.octree.buffer;
        int childOffset = thread.octree.memOffset;

        //children[i] = memOffset;
        setChildPointer(children[i], memOffset);
        setLeafMask(children[i], thread.octree.getLeafMask(0));

        childBuffer.position(NODE_SIZE).limit(childOffset); //we dont want the dummy head
        buffer.position(memOffset).put(childBuffer);
        memOffset += childOffset;

      }
      

      endTime = System.currentTimeMillis() - startTime;
      System.out.println("Octree generation elapsed time: " + endTime / 1000 + "s");
      System.out.println("memoffset: " + memOffset);
      System.out.println("usage: " + (float)memOffset / 1024 / 1024 + "MB");
    }

    // for(int i=0; i < 200; i++){
    //   System.out.println(buffer.get(i));
    // }
    //printBufferToFile("ManualTest.txt");
  }

  class Chunk {
    int[] origin;
    int pointer;
    public Chunk(int[] origin, int pointer){
      this.origin = origin;
      this.pointer = pointer;
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

  public byte getValue(int parentNode){
    return buffer.get(parentNode);
  }

  public void setValue(int parentNode, byte value){
    buffer.put(parentNode, value);
  }

  private byte getVoxel(ByteBuffer voxelData, int x, int y, int z){
    // int index = x + y * CHUNK_SIZE + z * Z_WIDTH;
    // y << 10 -> y * 1024, z << 20 -> z * 1024 * 1024
    return voxelData.get(x | (y << 10) | (z << 20));
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
          for(int i = cPos[n][2]-1; i <= cPos[n][2]+1; i++){
            if(i < 0 || i >= CHUNK_SIZE) continue;
            for(int j = cPos[n][1]-1; j <= cPos[n][1]+1; j++){
              if(j < 0 || j >= CHUNK_SIZE) continue;
              for(int k = cPos[n][0]-1; k <= cPos[n][0]+1; k++){
                if(k < 0 || k >= CHUNK_SIZE) continue;
                if(getVoxel(voxelData, k, j, i) == 0){
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
            if(i < 0 || i >= CHUNK_SIZE || i >= cPos[n][0] && i <= cPos[n][0]+cSize-1) continue;
            for(int j = cPos[n][1]-1; j <= cPos[n][1]+cSize; j++){
              if(j < 0 || j >= CHUNK_SIZE || j >= cPos[n][1] && j <= cPos[n][1]+cSize-1) continue;
              for(int k = cPos[n][2]-1; k <= cPos[n][2]+cSize; k++){
                if(k < 0 || k >= CHUNK_SIZE || k >= cPos[n][2] && k <= cPos[n][2]+cSize-1) continue;
                if(getVoxel(voxelData, i, j, k) == 0){
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
