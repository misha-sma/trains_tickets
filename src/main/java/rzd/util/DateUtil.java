package rzd.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateUtil {
	private static final Logger logger = LoggerFactory.getLogger(DateUtil.class);

	private DateUtil() {
	}

	public static final Map<Integer, String> DAY_OF_WEEK_MAP = new HashMap<Integer, String>();
	public static final Map<String, Integer> DAY_OF_WEEK_MAP_REVERSE = new HashMap<String, Integer>();
	public static final Map<String, String> DAY_OF_WEEK_MAP_ENG = new HashMap<String, String>();
	public static final Map<Integer, Integer> DAY_OF_WEEK_MINUTES_MAP = new HashMap<Integer, Integer>();

	public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	public static final SimpleDateFormat BIRTHDAY_FORMAT = new SimpleDateFormat("d.MM.yyyy");
	public static final SimpleDateFormat WEEK_DAY_FORMAT = new SimpleDateFormat("EEE");
	public static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	public static final SimpleDateFormat HEADER_TIME_FORMAT = new SimpleDateFormat("d.MM.yyyy EEE в HH:mm");
	public static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");

	public static final Pattern REPLACE_DAY_OF_WEEK_PATTERN = Pattern.compile("Mon|Tue|Wed|Thu|Fri|Sat|Sun");
	public static final String[] DAYS_OF_WEEK_ARRAY = new String[] { "вс", "пн", "вт", "ср", "чт", "пт", "сб" };

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

		DAY_OF_WEEK_MAP_ENG.put("Mon", "пн");
		DAY_OF_WEEK_MAP_ENG.put("Tue", "вт");
		DAY_OF_WEEK_MAP_ENG.put("Wed", "ср");
		DAY_OF_WEEK_MAP_ENG.put("Thu", "чт");
		DAY_OF_WEEK_MAP_ENG.put("Fri", "пт");
		DAY_OF_WEEK_MAP_ENG.put("Sat", "сб");
		DAY_OF_WEEK_MAP_ENG.put("Sun", "вс");

		DAY_OF_WEEK_MINUTES_MAP.put(1, 8640);
		DAY_OF_WEEK_MINUTES_MAP.put(2, 0);
		DAY_OF_WEEK_MINUTES_MAP.put(3, 1440);
		DAY_OF_WEEK_MINUTES_MAP.put(4, 2880);
		DAY_OF_WEEK_MINUTES_MAP.put(5, 4320);
		DAY_OF_WEEK_MINUTES_MAP.put(6, 5760);
		DAY_OF_WEEK_MINUTES_MAP.put(7, 7200);
	}

	public static String date2StringDots(Date date) {
		return BIRTHDAY_FORMAT.format(date);
	}

	public static Date string2Date(String date) {
		Date result = null;
		try {
			result = SIMPLE_DATE_FORMAT.parse(date);
		} catch (ParseException e) {
			logger.error(e.getMessage(), e);
		}
		return result;
	}

	public static Date string2DateTime(String date) {
		Date result = null;
		try {
			result = DATE_TIME_FORMAT.parse(date);
		} catch (ParseException e) {
			logger.error(e.getMessage(), e);
		}
		return result;
	}

	public static String addMinutesToDateDate(Date date, int minutes) {
		Calendar calendarDep = Calendar.getInstance();
		calendarDep.setTime(date);
		calendarDep.add(Calendar.MINUTE, minutes);
		return SIMPLE_DATE_FORMAT.format(calendarDep.getTime());
	}

	public static String addMinutesToDate(Date date, int minutes) {
		Calendar calendarDep = Calendar.getInstance();
		calendarDep.setTime(date);
		calendarDep.add(Calendar.MINUTE, minutes);
		return TIME_FORMAT.format(calendarDep.getTime());
	}

	public static String calcDestDate(String depDateTime, int delay) {
		Date d = string2DateTime(depDateTime);
		Calendar calendarDep = Calendar.getInstance();
		calendarDep.setTime(d);
		calendarDep.add(Calendar.MINUTE, delay);
		String dateEng = HEADER_TIME_FORMAT.format(calendarDep.getTime());
		return replaceDayOfWeek(dateEng);
	}

	private static String replaceDayOfWeek(String date) {
		Matcher m = REPLACE_DAY_OF_WEEK_PATTERN.matcher(date);
		if (m.find()) {
			return date.substring(0, m.start()) + DAY_OF_WEEK_MAP_ENG.get(m.group()) + date.substring(m.end());
		}
		return date;
	}

	public static String convertDateToDots(String date) {
		Date d = string2Date(date);
		return date2StringDots(d);
	}

	public static String getDayOfWeek(String date) {
		Date d = string2Date(date);
		String dayEng = WEEK_DAY_FORMAT.format(d);
		return DAY_OF_WEEK_MAP_ENG.get(dayEng);
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
