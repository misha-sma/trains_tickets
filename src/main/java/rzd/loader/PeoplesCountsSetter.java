package rzd.loader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rzd.persistence.dao.StationDao;
import rzd.util.Util;

public class PeoplesCountsSetter {
	private static final Logger logger = LoggerFactory.getLogger(PeoplesCountsSetter.class);

	public static void main(String[] args) {
		long initTime = System.currentTimeMillis();
		String path = "/home/misha-sma/Trains/stations-with-counts.txt";
		String text = Util.loadText(path);
		String[] lines = text.split("\n");
		for (String line : lines) {
			String[] parts = line.split("\\|");
			if (parts.length != 3) {
				continue;
			}
			int idStation = Integer.parseInt(parts[0].trim());
			int count = Integer.parseInt(parts[2].trim());
			StationDao.addPeoplesCount(idStation, count);
		}
		logger.info("ENDDDDDD!!!!!!! Time=" + (System.currentTimeMillis() - initTime) + " ms");
	}
}
