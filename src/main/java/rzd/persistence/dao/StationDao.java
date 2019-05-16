package rzd.persistence.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rzd.persistence.DBConnection;

public class StationDao {
	private static final Logger logger = LoggerFactory.getLogger(StationDao.class);

	public static final Map<String, Integer> STATIONS_NAME_ID_MAP = new HashMap<String, Integer>();
	public static final Map<Integer, String> STATIONS_ID_NAME_MAP = new HashMap<Integer, String>();

	public static final int MAX_SUGGESTIONS = 10;
	public static final Map<Character, Character> TRANSLIT_MAP = new HashMap<Character, Character>();
	public static final Map<String, List<String>> SUGGESTING_MAP = new HashMap<String, List<String>>();

	public static final int BATCH_SIZE = 1000;
	public static final String STATIONS_SQL = "SELECT id_station, name FROM stations ORDER BY peoples_count DESC LIMIT ? OFFSET ?";

	public static final String ADD_STATION_SQL = "INSERT INTO stations (name) VALUES (?) RETURNING id_station";
	public static final String ADD_PEOPLES_COUNT_SQL = "UPDATE stations SET peoples_count=? WHERE id_station=?";

	public static void loadStationsCaches() {
		int count = Integer.MAX_VALUE;
		int offset = 0;
		List<String> names = new LinkedList<String>();
		while (count >= BATCH_SIZE) {
			try (Connection con = DBConnection.getDbConnection();
					PreparedStatement ps = con.prepareStatement(STATIONS_SQL)) {
				ps.setInt(1, BATCH_SIZE);
				ps.setInt(2, offset);
				ResultSet rs = ps.executeQuery();
				count = 0;
				while (rs.next()) {
					++count;
					int idStation = rs.getInt(1);
					String name = rs.getString(2);
					STATIONS_NAME_ID_MAP.put(name.toLowerCase(), idStation);
					STATIONS_ID_NAME_MAP.put(idStation, name);
					names.add(name);
				}
				rs.close();
			} catch (ClassNotFoundException e) {
				logger.error(e.getMessage(), e);
			} catch (SQLException e) {
				logger.error(e.getMessage(), e);
			}
			offset += count;
		}
		createSuggestingMap(names);
		logger.info("Suggesting map size=" + SUGGESTING_MAP.size());
		setTranslitMap();
	}

	private static void createSuggestingMap(List<String> names) {
		for (String name : names) {
			for (int i = 1; i <= name.length(); ++i) {
				String prefix = name.substring(0, i).toLowerCase();
				List<String> value = SUGGESTING_MAP.get(prefix);
				if (value == null) {
					value = new LinkedList<String>();
					value.add(name);
					SUGGESTING_MAP.put(prefix, value);
					continue;
				}
				if (value.size() >= MAX_SUGGESTIONS) {
					continue;
				}
				value.add(name);
			}
		}
	}

	public static String translit(String str) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < str.length(); ++i) {
			char c = str.charAt(i);
			Character cEng = TRANSLIT_MAP.get(c);
			if (cEng == null) {
				return null;
			}
			builder.append(cEng);
		}
		return builder.toString();
	}

	private static void setTranslitMap() {
		TRANSLIT_MAP.put('q', 'й');
		TRANSLIT_MAP.put('w', 'ц');
		TRANSLIT_MAP.put('e', 'у');
		TRANSLIT_MAP.put('r', 'к');
		TRANSLIT_MAP.put('t', 'е');
		TRANSLIT_MAP.put('y', 'н');
		TRANSLIT_MAP.put('u', 'г');
		TRANSLIT_MAP.put('i', 'ш');
		TRANSLIT_MAP.put('o', 'щ');
		TRANSLIT_MAP.put('p', 'з');
		TRANSLIT_MAP.put('[', 'х');
		TRANSLIT_MAP.put(']', 'ъ');
		TRANSLIT_MAP.put('a', 'ф');
		TRANSLIT_MAP.put('s', 'ы');
		TRANSLIT_MAP.put('d', 'в');
		TRANSLIT_MAP.put('f', 'а');
		TRANSLIT_MAP.put('g', 'п');
		TRANSLIT_MAP.put('h', 'р');
		TRANSLIT_MAP.put('j', 'о');
		TRANSLIT_MAP.put('k', 'л');
		TRANSLIT_MAP.put('l', 'д');
		TRANSLIT_MAP.put(';', 'ж');
		TRANSLIT_MAP.put('\'', 'э');
		TRANSLIT_MAP.put('z', 'я');
		TRANSLIT_MAP.put('x', 'ч');
		TRANSLIT_MAP.put('c', 'с');
		TRANSLIT_MAP.put('v', 'м');
		TRANSLIT_MAP.put('b', 'и');
		TRANSLIT_MAP.put('n', 'т');
		TRANSLIT_MAP.put('m', 'ь');
		TRANSLIT_MAP.put(',', 'б');
		TRANSLIT_MAP.put('.', 'ю');
		TRANSLIT_MAP.put('`', 'ё');
		TRANSLIT_MAP.put(' ', ' ');
		TRANSLIT_MAP.put('-', '-');
		TRANSLIT_MAP.put('0', '0');
		TRANSLIT_MAP.put('1', '1');
		TRANSLIT_MAP.put('2', '2');
		TRANSLIT_MAP.put('3', '3');
		TRANSLIT_MAP.put('4', '4');
		TRANSLIT_MAP.put('5', '5');
		TRANSLIT_MAP.put('6', '6');
		TRANSLIT_MAP.put('7', '7');
		TRANSLIT_MAP.put('8', '8');
		TRANSLIT_MAP.put('9', '9');
	}

	public static int addStation(String name) {
		int idStation = -1;
		try (Connection con = DBConnection.getDbConnection();
				PreparedStatement ps = con.prepareStatement(ADD_STATION_SQL)) {
			ps.setString(1, name);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				idStation = rs.getInt(1);
			}
			rs.close();
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
		return idStation;
	}

	public static void addPeoplesCount(int idStation, int count) {
		try (Connection con = DBConnection.getDbConnection();
				PreparedStatement ps = con.prepareStatement(ADD_PEOPLES_COUNT_SQL)) {
			ps.setInt(1, count);
			ps.setInt(2, idStation);
			ps.execute();
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
	}
}
