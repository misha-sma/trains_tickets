package rzd.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
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

  public static String[] addMinutesToDateFull(Date date, int minutes) {
    String[] result = new String[2];
    Calendar calendarDep = Calendar.getInstance();
    calendarDep.setTime(date);
    calendarDep.add(Calendar.MINUTE, minutes);
    result[0] = String.valueOf(calendarDep.get(Calendar.DAY_OF_MONTH)) + "."
        + addZeros2(calendarDep.get(Calendar.MONTH) + 1) + "." + calendarDep.get(Calendar.YEAR);
    result[1] = addZeros2(calendarDep.get(Calendar.HOUR_OF_DAY)) + ":" + addZeros2(calendarDep.get(Calendar.MINUTE));
    return result;
  }

  public static String addMinutesToDate(Date date, int minutes) {
    Calendar calendarDep = Calendar.getInstance();
    calendarDep.setTime(date);
    calendarDep.add(Calendar.MINUTE, minutes);
    String depTime = addZeros2(calendarDep.get(Calendar.HOUR_OF_DAY)) + ":" + addZeros2(calendarDep.get(Calendar.MINUTE));
    return depTime;
  }

  public static String[] calcDestDate(String depDate, String depTime, int delay) {
    String[] result = new String[2];
    String[] parts = depDate.split("-");
    String[] partsH = depTime.split(":");
    Calendar calendarDep = Calendar.getInstance();
    calendarDep.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]),
        Integer.parseInt(partsH[0]), Integer.parseInt(partsH[1]));
    calendarDep.add(Calendar.MINUTE, delay);
    result[0] = String.valueOf(calendarDep.get(Calendar.DAY_OF_MONTH)) + "."
        + addZeros2(calendarDep.get(Calendar.MONTH) + 1) + "." + calendarDep.get(Calendar.YEAR);
    result[1] = addZeros2(calendarDep.get(Calendar.HOUR_OF_DAY)) + ":" + addZeros2(calendarDep.get(Calendar.MINUTE));
    return result;
  }

  public static String convertDateToDots(String date) {
    String[] parts = date.split("-");
    String day = parts[2].startsWith("0") ? parts[2].substring(1) : parts[2];
    return day + "." + parts[1] + "." + parts[0];
  }
  
  public static String addZeros2(int number) {
    return number < 10 ? "0" + number : String.valueOf(number);
  }

  public static String formatMinutes(int minutes) {
    int days = minutes / (24 * 60);
    int hours = (minutes % (24 * 60)) / 60;
    int minutesTrue = minutes % 60;
    StringBuilder builder = new StringBuilder();
    if (days > 0) {
      builder.append(days).append(" д ");
    }
    if (days > 0 || hours > 0) {
      builder.append(hours).append(" ч ");
    }
    builder.append(minutesTrue).append(" мин");
    return builder.toString();
  }
}
