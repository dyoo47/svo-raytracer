package src.engine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

public class Octree {
  ByteBuffer buffer;
  int memOffset = 0;
  int bufferSize = 0;

  long surfaceLeafNodes = 0;
  long nonSurfaceLeafNodes = 0;
  long interiorNodes = 0;

  static final int NODE_SIZE = 7;
  static final int LEAF_SIZE = 3;
  static final int NON_SURFACE_LEAF_SIZE = 1;
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
  NEW NODE STRUCTURE
  branch - Leaf Tag: 0
  0 :: value - 1 byte
  1 :: child pointer - 4 bytes
  2 ::
  3 ::
  4 ::
  5 :: leaf mask - 2 bytes
  6 ::
  
  surface leaf - Leaf Tag: 1
  0 :: value - 1 byte
  1 :: normal - 2 bytes
  2 :: 

  transparent leaf / non-surface leaf - Leaf Tag: 3
  0 :: value - 1 byte

  flagged branch (unused) - Leaf Tag: 2
  0 :: value - 1 byte
  */

  public void createDummyHead(){
    //TODO: Add error handling
    createInteriorNode((byte)1);
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

  private int createInteriorNode(byte val){
    interiorNodes++;
    int pointer = memOffset;
    buffer.put(memOffset++, val);
    buffer.put(memOffset++, (byte)0);
    buffer.put(memOffset++, (byte)0);
    buffer.put(memOffset++, (byte)0);
    buffer.put(memOffset++, (byte)0);
    buffer.put(memOffset++, (byte)0);
    buffer.put(memOffset++, (byte)0);
    return pointer;
  }

  private int createSurfaceLeafNode(byte val, short normal){
    surfaceLeafNodes++;
    int pointer = memOffset;
    buffer.put(memOffset++, val);
    buffer.put(memOffset++, (byte)(normal));
    buffer.put(memOffset++, (byte)(normal >> 8));
    return pointer;
  }

  private int createNonSurfaceLeafNode(byte val){
    nonSurfaceLeafNodes++;
    int pointer = memOffset;
    buffer.put(memOffset++, val);
    return pointer;
  }

  private void setChildPointer(int parentPointer, int childPointer){
    buffer.putInt(parentPointer + 1, childPointer - parentPointer);
  }

  private void setLeafMask(int parentPointer, short leafMask){
    buffer.putShort(parentPointer + 5, leafMask);
  }

  private short getLeafMask(int parentPointer){
    return buffer.getShort(parentPointer + 5);
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

  public void constructCompleteOctree(Renderer.Shader chunkGenShader, int voxelTexture, int heightmapTexture, int materialTexture){

    double startTotalTime = System.currentTimeMillis();
    Renderer renderer = Renderer.getInstance();
    ShortBuffer heightmapBuffer;
    ByteBuffer matmapBuffer;

    try(MemoryStack stack = MemoryStack.stackPush()){

      IntBuffer width = stack.mallocInt(1);
      IntBuffer height = stack.mallocInt(1);
      IntBuffer channels = stack.mallocInt(1);

      File heightmapFile = new File("./assets/heightmaps/nz.png");
      String heightmapFilePath = heightmapFile.getAbsolutePath();
      heightmapBuffer = STBImage.stbi_load_16(heightmapFilePath, width, height, channels, 1);
      if(heightmapBuffer == null){
        throw new Exception("Can't load file " + STBImage.stbi_failure_reason());
      }
      renderer.buffer2DTexture(heightmapTexture, 4, width.get(0), height.get(0), heightmapBuffer);
      STBImage.stbi_image_free(heightmapBuffer);

      File matmapFile = new File("./assets/matmaps/materials.png");
      String matmapFilePath = matmapFile.getAbsolutePath();
      matmapBuffer = STBImage.stbi_load(matmapFilePath, width, height, channels, 1);
      if(matmapBuffer == null){
        throw new Exception("Can't load file " + STBImage.stbi_failure_reason());
      }
      renderer.buffer2DTexture(materialTexture, 5, width.get(0), height.get(0), matmapBuffer);
      STBImage.stbi_image_free(matmapBuffer);

    }catch(Exception e){
      e.printStackTrace();
    }

    int[] rootPos = {0, -1024, 0};
    // construct root
    createInteriorNode((byte) 1);

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
        maxLOD = 7;
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

      int numGroupsEachAxis = CHUNK_SIZE / Constants.COMPUTE_GROUP_SIZE;

      renderer.useProgram(chunkGenShader);
      renderer.setUniformInteger(1, chunk.origin[0]);
      renderer.setUniformInteger(2, chunk.origin[1]);
      renderer.setUniformInteger(3, chunk.origin[2]);

      renderer.printGLErrors();
      renderer.dispatchCompute(chunkGenShader, numGroupsEachAxis, numGroupsEachAxis, numGroupsEachAxis);

      ByteBuffer voxelBuffer = BufferUtils.createByteBuffer(CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE);

      renderer.printGLErrors();
      renderer.get3DTextureData(voxelTexture, 3, voxelBuffer);

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
        threads[i] = new OctreeThread(cPos[i], voxelBuffer, maxLOD);
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
        children[i] = createInteriorNode((byte) 1);
      }
      setChildPointer(chunk.pointer, children[0]);

      for(int i=0; i < 8; i++){

        OctreeThread thread = threads[i];
        ByteBuffer childBuffer = thread.octree.buffer;
        int childOffset = thread.octree.memOffset;
        short headLeafMask = thread.octree.getLeafMask(0);

        // Set child pointer to start of new buffer, copy over leaf mask from dummy head
        setChildPointer(children[i], memOffset);
        setLeafMask(children[i], headLeafMask);

        // Copy over buffer minus dummy head
        childBuffer.position(NODE_SIZE).limit(childOffset);
        buffer.position(memOffset).put(childBuffer);
        memOffset += childOffset;

        // Copy over debug info
        surfaceLeafNodes += thread.octree.surfaceLeafNodes;
        nonSurfaceLeafNodes += thread.octree.nonSurfaceLeafNodes;
        interiorNodes += thread.octree.interiorNodes;
      }
      

      endTime = System.currentTimeMillis() - startTime;
      System.out.println("Octree generation elapsed time: " + endTime / 1000 + "s");
      System.out.println("memoffset: " + memOffset);
      System.out.println("usage: " + (float)memOffset / 1024 / 1024 + "MB");
    }
    double endTotalTime = System.currentTimeMillis() - startTotalTime;
    System.out.println("Done! Total time: " + endTotalTime / 1000 + "s" + 
      " | Avg. time: " + endTotalTime / chunks.size() / 1000 + "s");
  }

  public void constructCompleteOctree(Renderer.Shader chunkGenShader, int texture){

    double startTotalTime = System.currentTimeMillis();
    int[] rootPos = {0, -1024, 0};
    // construct root
    createInteriorNode((byte) 1);

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
        maxLOD = 7;
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
      int numGroupsEachAxis = CHUNK_SIZE / Constants.COMPUTE_GROUP_SIZE;

      renderer.useProgram(chunkGenShader);
      renderer.setUniformInteger(1, chunk.origin[0]);
      renderer.setUniformInteger(2, chunk.origin[1]);
      renderer.setUniformInteger(3, chunk.origin[2]);

      renderer.printGLErrors();
      renderer.dispatchCompute(chunkGenShader, numGroupsEachAxis, numGroupsEachAxis, numGroupsEachAxis);

      ByteBuffer voxelBuffer = BufferUtils.createByteBuffer(CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE);

      renderer.printGLErrors();
      renderer.get3DTextureData(texture, 3, voxelBuffer);

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
        threads[i] = new OctreeThread(cPos[i], voxelBuffer, maxLOD);
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
        children[i] = createInteriorNode((byte) 1);
      }
      setChildPointer(chunk.pointer, children[0]);

      for(int i=0; i < 8; i++){

        OctreeThread thread = threads[i];
        ByteBuffer childBuffer = thread.octree.buffer;
        int childOffset = thread.octree.memOffset;
        short headLeafMask = thread.octree.getLeafMask(0);

        // Set child pointer to start of new buffer, copy over leaf mask from dummy head
        setChildPointer(children[i], memOffset);
        setLeafMask(children[i], headLeafMask);

        // Copy over buffer minus dummy head
        childBuffer.position(NODE_SIZE).limit(childOffset);
        buffer.position(memOffset).put(childBuffer);
        memOffset += childOffset;

        // Copy over debug info
        surfaceLeafNodes += thread.octree.surfaceLeafNodes;
        nonSurfaceLeafNodes += thread.octree.nonSurfaceLeafNodes;
        interiorNodes += thread.octree.interiorNodes;
      }
      

      endTime = System.currentTimeMillis() - startTime;
      System.out.println("Octree generation elapsed time: " + endTime / 1000 + "s");
      System.out.println("memoffset: " + memOffset);
      System.out.println("usage: " + (float)memOffset / 1024 / 1024 + "MB");
    }
    double endTotalTime = System.currentTimeMillis() - startTotalTime;
    System.out.println("Done! Total time: " + endTotalTime / 1000 + "s" + 
      " | Avg. time: " + endTotalTime / chunks.size() / 1000 + "s");
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
      children[i] = createInteriorNode((byte) 1);
    }
    for(int i=0; i < 8; i++){
      fillEmptyChildren(children[i], levels - 1, cPos[i], chunks);
    }
    setChildPointer(parentPointer, children[0]);
  }
  
  public void constructInnerOctree(int size, int curLOD, int maxLOD, int[] pPos, int parentPointer, ByteBuffer voxelData){

    int cSize = size / 2;
    if(cSize == 0 || curLOD == maxLOD) return;

    int[] children = {0, 0, 0, 0, 0, 0, 0, 0};
    int[][] cPos = new int[8][3];
    for(int n = 0; n < 8; n++){
      cPos[n][0] = pPos[0] + childOffsets[n][0] * cSize;
      cPos[n][1] = pPos[1] + childOffsets[n][1] * cSize;
      cPos[n][2] = pPos[2] + childOffsets[n][2] * cSize;
    }
    short leafMask = 0;
    for(int n = 0; n < 8; n++){
      byte first = getVoxel(voxelData, cPos[n][0], cPos[n][1], cPos[n][2]);
      byte value = first;
      boolean leaf = true;
      boolean nonSurface = false;
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
      if(leaf && value != 0) {
        if(cSize == 1){
          NormalResult normalResult = genSurfaceNormal(cPos, n, voxelData);
          if(normalResult.exposed){
            children[n] = createSurfaceLeafNode(value, normalResult.normal);
          }else{
            children[n] = createNonSurfaceLeafNode(value);
            nonSurface = true;
          }
        }else{
          if(checkBigNodeExposed(cPos, cSize, n, voxelData)){
            leaf = false;
            children[n] = createInteriorNode(value);
          }else{
            children[n] = createNonSurfaceLeafNode(value);
            nonSurface = true;
          }
        }
      }else if(leaf){
        children[n] = createNonSurfaceLeafNode(value);
        nonSurface = true;
      }
      else children[n] = createInteriorNode(value);
      if(leaf)       leafMask |= (0x0001 << (n << 1));
      if(nonSurface) leafMask |= (0x0002 << (n << 1));
    }
    setChildPointer(parentPointer, children[0]);
    setLeafMask(parentPointer, leafMask);
    for(int n = 0; n < 8; n++){
      if(getValue(children[n]) != 0 && (leafMask & (0x0001 << (n << 1))) == 0){
        constructInnerOctree(cSize, curLOD + 1, maxLOD, cPos[n], children[n], voxelData);
      }
    }
  }

  class NormalResult {
    boolean exposed;
    short normal;
    public NormalResult(short normal, boolean exposed){
      this.normal = normal;
      this.exposed = exposed;
    }
  }

  private NormalResult genSurfaceNormal(int[][] cPos, int localChild, ByteBuffer voxelData){
    int n = localChild;
    boolean exposed = false;
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
            exposed = true;
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
    return new NormalResult(packed, exposed);
  }

  private boolean checkBigNodeExposed(int[][] cPos, int cSize, int localChild, ByteBuffer voxelData){
    int n = localChild;
    boolean exposed = false;
    for(int i = cPos[n][2]-1; i <= cPos[n][2]+cSize+1; i++){
      if(i < 0 || i >= CHUNK_SIZE || i >= cPos[n][2] && i <= cPos[n][2]+cSize-1) continue;
      for(int j = cPos[n][1]-1; j <= cPos[n][1]+cSize+1; j++){
        if(j < 0 || j >= CHUNK_SIZE || j >= cPos[n][1] && j <= cPos[n][1]+cSize-1) continue;
        for(int k = cPos[n][0]-1; k <= cPos[n][0]+cSize+1; k++){
          if(k < 0 || k >= CHUNK_SIZE || k >= cPos[n][0] && k <= cPos[n][0]+cSize-1) continue;
          if(getVoxel(voxelData, k, j, i) == 0){
            exposed = true;
          }
        }
      }
    }
    return exposed;
  }
  
  public ByteBuffer getByteBuffer(){
    return buffer;
  }

  public void writeBufferToFile(String fileName){
    buffer.position(0);
    try{
      File outfile = new File(Constants.MAP_DIR + fileName);
      ByteBuffer buf = this.getByteBuffer();
      buf.limit(memOffset);
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

  public void printNodeCounts(){
    DecimalFormat df = new DecimalFormat("###,###,###,###,###,###");
    System.out.println("Surface Leaves: " + df.format(surfaceLeafNodes));
    System.out.println("Non-Surface Leaves: " + df.format(nonSurfaceLeafNodes));
    System.out.println("Interior Nodes: " + df.format(interiorNodes));
    long total = surfaceLeafNodes + nonSurfaceLeafNodes + interiorNodes;
    System.out.println("Total: " + df.format(total));
  }

}
