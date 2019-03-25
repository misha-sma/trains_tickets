package rzd.persistence.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rzd.persistence.DBConnection;

public class StationDao {
	private static final Logger logger = LoggerFactory.getLogger(StationDao.class);

	public static final Map<String, Integer> STATIONS_NAME_ID_MAP = new HashMap<String, Integer>();
	public static final Map<Integer, String> STATIONS_ID_NAME_MAP = new HashMap<Integer, String>();

	public static final int BATCH_SIZE = 1000;
	public static final String STATIONS_SQL = "SELECT * FROM stations ORDER BY id_station LIMIT ? OFFSET ?";

	public static void loadStationsCaches() {
		int count = Integer.MAX_VALUE;
		int offset = 0;
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
				}
				rs.close();
			} catch (ClassNotFoundException e) {
				logger.error(e.getMessage(), e);
			} catch (SQLException e) {
				logger.error(e.getMessage(), e);
			}
			offset += count;
		}
	}

}
