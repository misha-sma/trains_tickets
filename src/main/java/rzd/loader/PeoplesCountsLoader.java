package rzd.loader;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rzd.persistence.dao.StationDao;
import rzd.util.Util;

public class PeoplesCountsLoader {
	private static final Logger logger = LoggerFactory.getLogger(PeoplesCountsLoader.class);

	public static void main(String[] args) {
		long initTime = System.currentTimeMillis();
		StationDao.loadStationsCaches();
		String path = "/home/misha-sma/Trains/perepis-2010-true.txt";
		String text = Util.loadText(path);
		String[] lines = text.split("\n");
		Map<String, String> countsMap = new HashMap<String, String>();
		for (String line : lines) {
			String[] parts = line.split("\\|");
			if (parts.length != 2) {
				continue;
			}
			String name = parts[0].trim();
			String counts = parts[1].trim();
			countsMap.put(name, counts);
		}
		Set<Integer> idSet = StationDao.STATIONS_ID_NAME_MAP.keySet();
		List<Integer> idList = new ArrayList<Integer>();
		idList.addAll(idSet);
		Collections.sort(idList);
		try (FileWriter fw = new FileWriter("/home/misha-sma/Trains/stations-with-counts-222.txt")) {
			for (int idStation : idList) {
				String name = StationDao.STATIONS_ID_NAME_MAP.get(idStation);
				String counts = countsMap.get(name);
				counts = counts == null ? "0" : counts;
				fw.write(String.valueOf(idStation) + " | " + name + " | " + counts + "\n");
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		logger.info("ENDDDDDD!!!!!!! Time=" + (System.currentTimeMillis() - initTime) + " ms");
	}
}
