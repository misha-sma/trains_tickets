package rzd.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {
	private static final Logger logger = LoggerFactory.getLogger(Util.class);

	private Util() {
	}

	public static String loadText(String path) {
		return loadText(new File(path));
	}

	public static String loadText(File file) {
		try (FileInputStream in = new FileInputStream(file)) {
			return IOUtils.toString(in, "UTF-8");
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	public static String loadTextWithResourceAsStream(String path) {
		try {
			return IOUtils.resourceToString(path, Charset.forName("UTF-8"));
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	public static byte[] loadBytesWithResourceAsStream(String path) {
		try {
			return IOUtils.resourceToByteArray(path);
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
