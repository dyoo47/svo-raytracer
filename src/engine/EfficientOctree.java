import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;

public class EfficientOctree {
  byte[] mem;
  ByteBuffer buffer;
  int memOffset = 0;
  int bufferSize = 0;

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

  public EfficientOctree(int memSizeKB, VoxelData voxelData){
    bufferSize = memSizeKB * 1024;
    buffer = ByteBuffer.allocate(bufferSize);
    mem = buffer.array();
    createNode((byte) 1);
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

  public void constructOctree(VoxelData data, int maxLOD){
    int maxSize = 1 << maxLOD;
    createNode((byte) 0); //value shouldn't be read cuz root is never leaf node
    int[] rootPos = {0, 0, 0};
    constructOctree(data, maxSize, 0, rootPos, 0);
  }

  public byte getValue(int parentNode){
    return mem[parentNode];
  }

  private void constructOctree(VoxelData data, int maxSize, int curLOD, int[] pPos, int parentPointer){

    int cSize = maxSize >> curLOD;
    if(cSize == 0) return;

    byte leafMask = 0;
    int[][] cPos = new int[8][3];
    int[] children = {0, 0, 0, 0, 0, 0, 0, 0};
    for(int n = 0; n < 8; n++){
      cPos[n][0] = pPos[0] + childOffsets[n][0] * cSize;
      cPos[n][1] = pPos[1] + childOffsets[n][1] * cSize;
      cPos[n][2] = pPos[2] + childOffsets[n][2] * cSize;
    }

    for(int n = 0; n < 8; n++){
      
      byte first = data.get(cPos[n][0], cPos[n][1], cPos[n][2]);
      byte value = first;
      boolean leaf = true;
      for(int i = cPos[n][0]; i < cPos[n][0] + cSize; i++){
        for(int j = cPos[n][1]; j < cPos[n][1] + cSize; j++){
          for(int k = cPos[n][2]; k < cPos[n][2] + cSize; k++){
            byte sample = data.get(i, j, k);
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
        constructOctree(data, maxSize, curLOD + 1, cPos[n], children[n]);
      }
    }
  }
  
  public ByteBuffer getByteBuffer(){
    ByteBuffer out = BufferUtils.createByteBuffer(memOffset + 1024);
    for(int i=0; i < memOffset; i++){
      out.put(i, buffer.get(i));
    }
    return out;
  }
}
