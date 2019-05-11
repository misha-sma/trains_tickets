package rzd.loader;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rzd.persistence.dao.StationDao;
import rzd.persistence.dao.TrainDao;
import rzd.util.Util;

public class TrainsLoader {
	private static final Logger logger = LoggerFactory.getLogger(TrainsLoader.class);

	public static final Pattern MINUTES_PATTERN = Pattern.compile("(\\d+) мин");
	public static final Pattern HOURS_PATTERN = Pattern.compile("(\\d+) ч");
	public static final Pattern DAYS_PATTERN = Pattern.compile("(\\d+) дн");

	public static void main(String[] args) {
		StationDao.loadStationsCaches();

		String path = "/home/misha-sma/Trains/yandex-crawler/files/trains/";
		File folder = new File(path);
		for (File file : folder.listFiles()) {
			logger.info("file=" + file.getName());
			String text = Util.loadText(file);
			String[] lines = text.split("\n");
			int idTrain = Integer.parseInt(lines[0].trim());
			String departureDays = lines[1].trim();
			String name = lines[2].trim();
			name = name.isEmpty() ? null : name;
			String departureTime = lines[3].trim() + ":00";
			TrainDao.addTrain(idTrain, name, departureTime, departureDays);
			for (int i = 4; i < lines.length; ++i) {
				String[] parts = lines[i].split("\\|");
				String station = parts[0].trim();
				int stayTime = 0;
				Matcher m = MINUTES_PATTERN.matcher(parts[1]);
				if (m.find()) {
					stayTime = Integer.parseInt(m.group(1));
				}
				m = HOURS_PATTERN.matcher(parts[1]);
				if (m.find()) {
					stayTime += 60 * Integer.parseInt(m.group(1));
				}
				if (stayTime == 0) {
					stayTime = Integer.parseInt(parts[1].trim());
				}
				
				int travelTime = 0;
				m = MINUTES_PATTERN.matcher(parts[2]);
				if (m.find()) {
					travelTime = Integer.parseInt(m.group(1));
				}
				m = HOURS_PATTERN.matcher(parts[2]);
				if (m.find()) {
					travelTime += 60 * Integer.parseInt(m.group(1));
				}
				m = DAYS_PATTERN.matcher(parts[2]);
				if (m.find()) {
					travelTime += 24 * 60 * Integer.parseInt(m.group(1));
				}
				if (i == 4) {
					stayTime = 0;
					travelTime = 0;
				}
				Integer idStation = StationDao.STATIONS_NAME_ID_MAP.get(station.toLowerCase());
				if (idStation == null) {
					idStation = StationDao.addStation(station);
					StationDao.STATIONS_NAME_ID_MAP.put(station.toLowerCase(), idStation);
					StationDao.STATIONS_ID_NAME_MAP.put(idStation, station);
				}
				TrainDao.addTrainStation(idTrain, idStation, travelTime, stayTime);
				if (stayTime == -1) {
					break;
				}
			}
		}
		logger.info("ENDDDDDDDDDD!!!!!!!");
	}
}
