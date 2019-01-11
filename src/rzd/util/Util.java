package rzd.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Util {
  private Util() {
  }

  public static String loadText(File file) {
      try {
          FileInputStream input = new FileInputStream(file);
          byte[] bytes = new byte[(int) file.length()];
          input.read(bytes);
          input.close();
          return new String(bytes);
      } catch (FileNotFoundException e) {
          //logger.error(e);
          e.printStackTrace();
      } catch (IOException e) {
          //logger.error(e);
        e.printStackTrace();
      }
      return null;
  }

  public static String loadText(String fileName) {
      return loadText(new File(fileName));
  }
}
