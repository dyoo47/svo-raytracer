import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Shader {
    public static String readFromFile(String filePath){
        String content = "";
        try{
            content = Files.readString(Paths.get(filePath));
        } catch (IOException e){
            e.printStackTrace();
        }
        return content;
    }
}
