package rzd.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

  public static Map<String, String> parseParameters(String url) {
    Map<String, String> result = new HashMap<String, String>();
    String[] parts = url.split("&|\\?");
    for (String part : parts) {
      int index = part.indexOf('=');
      if (index <= 0) {
        continue;
      }
      String key = part.substring(0, index).trim();
      String value = part.substring(index + 1).trim();
      if (!key.isEmpty()) {
        result.put(key, value);
      }
    }
    return result;
  }
}
