package tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Utils {
    public static long getFileSize(String path) {
        FileInputStream fis  = null;
        try {
            File file = new File(path);
            String fileName = file.getName();
            fis = new FileInputStream(file);
            return fis.available();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
