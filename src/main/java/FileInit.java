import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileInit {
    public static void createPath(){
        try {
            Files.createDirectories(Path.of("data" + File.separator  + "received_files/"));
        } catch (IOException e) {
            System.out.println("Произошла ошибки при создании: " + e.getMessage());
        }

        try {
            Files.createDirectories(Path.of("data" + File.separator  + "logs" + File.separator));
        } catch (IOException e) {
            System.out.println("Произошла ошибки при создании: " + e.getMessage());
        }
    }
}
