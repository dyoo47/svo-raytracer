package src.engine;

public class Material {

  private static Material[] materials;
  private static int numMats;

  public byte value;
  public String name;
  public int type;
  public String matmapFilePath;

  public Material(byte value, String name, int type) {
    this.value = value;
    this.name = name;
    this.type = type;
    matmapFilePath = null;
  }

  public Material(byte value, String name, int type, String matmapFilePath) {
    this.value = value;
    this.name = name;
    this.type = type;
    this.matmapFilePath = matmapFilePath;
  }

  public boolean hasMatMap() {
    return matmapFilePath != null;
  }

  public static int getNumMats() {
    return numMats;
  }

  public static Material getMaterial(int id) {
    return materials[id];
  }

  public static void initMaterials() {
    materials = new Material[Constants.MAX_MATERIALS];
    numMats = 0;
    materials[0] = new Material((byte) numMats++, "air", 1);
    materials[1] = new Material((byte) numMats++, "stone", 1, "./assets/matmaps/nz/stone.png");
    materials[2] = new Material((byte) numMats++, "scree", 1, "./assets/matmaps/nz/scree.png");
    materials[3] = new Material((byte) numMats++, "grass", 1, "./assets/matmaps/nz/grass.png");
  }
}
