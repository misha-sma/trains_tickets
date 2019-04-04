package rzd.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {
	private static final Logger logger = LoggerFactory.getLogger(Util.class);

	private Util() {
	}

	public static final Map<Integer, String> DAY_OF_WEEK_MAP = new HashMap<Integer, String>();
	public static final Map<String, Integer> DAY_OF_WEEK_MAP_REVERSE = new HashMap<String, Integer>();

	static {
		DAY_OF_WEEK_MAP.put(1, "вс");
		DAY_OF_WEEK_MAP.put(2, "пн");
		DAY_OF_WEEK_MAP.put(3, "вт");
		DAY_OF_WEEK_MAP.put(4, "ср");
		DAY_OF_WEEK_MAP.put(5, "чт");
		DAY_OF_WEEK_MAP.put(6, "пт");
		DAY_OF_WEEK_MAP.put(7, "сб");

		DAY_OF_WEEK_MAP_REVERSE.put("вс", 1);
		DAY_OF_WEEK_MAP_REVERSE.put("пн", 2);
		DAY_OF_WEEK_MAP_REVERSE.put("вт", 3);
		DAY_OF_WEEK_MAP_REVERSE.put("ср", 4);
		DAY_OF_WEEK_MAP_REVERSE.put("чт", 5);
		DAY_OF_WEEK_MAP_REVERSE.put("пт", 6);
		DAY_OF_WEEK_MAP_REVERSE.put("сб", 7);
	}

	public static String loadTextWithResourceAsStream(String path) {
		try (InputStream inputStream = Util.class.getResourceAsStream(path)) {
			return IOUtils.toString(inputStream, "UTF-8");
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	public static byte[] loadBytesWithResourceAsStream(String path) {
		try (InputStream inputStream = Util.class.getResourceAsStream(path)) {
			return IOUtils.toByteArray(inputStream);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return null;
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

	public static String addMinutesToDateDate(Date date, int minutes) {
		Calendar calendarDep = Calendar.getInstance();
		calendarDep.setTime(date);
		calendarDep.add(Calendar.MINUTE, minutes);
		return String.valueOf(calendarDep.get(Calendar.YEAR)) + "-" + addZeros2(calendarDep.get(Calendar.MONTH) + 1)
				+ "-" + addZeros2(calendarDep.get(Calendar.DAY_OF_MONTH));
	}

	public static String[] addMinutesToDateFull(Date date, int minutes) {
		String[] result = new String[2];
		Calendar calendarDep = Calendar.getInstance();
		calendarDep.setTime(date);
		calendarDep.add(Calendar.MINUTE, minutes);
		result[0] = String.valueOf(calendarDep.get(Calendar.DAY_OF_MONTH)) + "."
				+ addZeros2(calendarDep.get(Calendar.MONTH) + 1) + "." + calendarDep.get(Calendar.YEAR);
		result[1] = addZeros2(calendarDep.get(Calendar.HOUR_OF_DAY)) + ":"
				+ addZeros2(calendarDep.get(Calendar.MINUTE));
		return result;
	}

	public static String addMinutesToDate(Date date, int minutes) {
		Calendar calendarDep = Calendar.getInstance();
		calendarDep.setTime(date);
		calendarDep.add(Calendar.MINUTE, minutes);
		String depTime = addZeros2(calendarDep.get(Calendar.HOUR_OF_DAY)) + ":"
				+ addZeros2(calendarDep.get(Calendar.MINUTE));
		return depTime;
	}

	public static String[] calcDestDate(String depDate, String depTime, int delay) {
		String[] result = new String[3];
		String[] parts = depDate.split("-");
		String[] partsH = depTime.split(":");
		Calendar calendarDep = Calendar.getInstance();
		calendarDep.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]),
				Integer.parseInt(partsH[0]), Integer.parseInt(partsH[1]));
		calendarDep.add(Calendar.MINUTE, delay);
		result[0] = String.valueOf(calendarDep.get(Calendar.DAY_OF_MONTH)) + "."
				+ addZeros2(calendarDep.get(Calendar.MONTH) + 1) + "." + calendarDep.get(Calendar.YEAR);
		result[1] = addZeros2(calendarDep.get(Calendar.HOUR_OF_DAY)) + ":"
				+ addZeros2(calendarDep.get(Calendar.MINUTE));
		result[2] = DAY_OF_WEEK_MAP.get(calendarDep.get(Calendar.DAY_OF_WEEK));
		return result;
	}

	public static String convertDateToDots(String date) {
		String[] parts = date.split("-");
		String day = parts[2].startsWith("0") ? parts[2].substring(1) : parts[2];
		return day + "." + parts[1] + "." + parts[0];
	}

	public static String getDayOfWeek(String date) {
		String[] parts = date.split("-");
		Calendar calendarDep = Calendar.getInstance();
		calendarDep.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]));
		int dayOfWeek = calendarDep.get(Calendar.DAY_OF_WEEK);
		return DAY_OF_WEEK_MAP.get(dayOfWeek);
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

	public static String getSeatType(int seatNumber, int carriageType) {
		if (carriageType == 1) {
			// плацкарт
			if (seatNumber <= 32) {
				if (seatNumber % 2 == 1) {
					return "нижнее";
				} else {
					return "верхнее";
				}
			} else if (seatNumber >= 39) {
				if (seatNumber % 2 == 1) {
					return "нижнее боковое";
				} else {
					return "верхнее боковое";
				}
			} else if (seatNumber >= 33 && seatNumber <= 36) {
				if (seatNumber % 2 == 1) {
					return "нижнее у туалета";
				} else {
					return "верхнее у туалета";
				}
			} else if (seatNumber >= 37 && seatNumber <= 38) {
				if (seatNumber % 2 == 1) {
					return "нижнее боковое у туалета";
				} else {
					return "верхнее боковое у туалета";
				}
			}
		} else if (carriageType == 2) {
			// купе
			if (seatNumber % 2 == 1) {
				return "нижнее";
			} else {
				return "верхнее";
			}
		} else if (carriageType == 3) {
			// СВ
			return "нижнее";
		} else if (carriageType == 4) {
			// сидячий
			if (seatNumber % 2 == 1) {
				return "у окна";
			} else {
				return "у прохода";
			}
		} else if (carriageType == 5) {
			// двухэтажный
			if (seatNumber <= 32) {
				if (seatNumber % 2 == 1) {
					return "нижнее первый этаж";
				} else {
					return "верхнее первый этаж";
				}
			} else {
				if (seatNumber % 2 == 1) {
					return "нижнее второй этаж";
				} else {
					return "верхнее второй этаж";
				}
			}
		}
		return "";
	}

}
