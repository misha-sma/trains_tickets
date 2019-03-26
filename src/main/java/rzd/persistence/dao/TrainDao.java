package rzd.persistence.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rzd.persistence.DBConnection;
import rzd.persistence.entity.Train;
import rzd.persistence.entity.TrainTravelStayTimes;

public class TrainDao {
	private static final Logger logger = LoggerFactory.getLogger(TrainDao.class);

	public static final String TRAINS_ALL_DAYS_SQL = "SELECT t1.id_train, t1.travel_time+t1.stay_time, t2.travel_time FROM trains_stations "
			+ "AS t1 INNER JOIN trains_stations AS t2 ON t1.id_station=? AND t2.id_station=? AND t1.id_train=t2.id_train "
			+ "AND t1.travel_time<t2.travel_time ORDER BY t1.id_train";

	public static final String TRAINS_DATE_SQL = "SELECT DISTINCT carriages.id_train, t3.travel_stay_time, t3.travel_time FROM carriages "
			+ "INNER JOIN (SELECT t1.id_train, t1.travel_time+t1.stay_time AS travel_stay_time, t2.travel_time "
			+ "FROM trains_stations AS t1 INNER JOIN trains_stations AS t2 ON t1.id_station=? AND t2.id_station=? AND "
			+ "t1.id_train=t2.id_train AND t1.travel_time<t2.travel_time) AS t3 ON "
			+ "(carriages.id_train=t3.id_train AND departure_time+t3.travel_stay_time*interval '1 minute'>='?"
			+ " 00:00:00' AND departure_time+t3.travel_stay_time*interval '1 minute'<='?"
			+ " 23:59:59') ORDER BY carriages.id_train";

	public static final int BATCH_SIZE = 1000;
	public static final String TRAINS_SQL = "SELECT t1.*, trains_stations.id_station FROM "
			+ "(SELECT trains.*, trains_stations.id_station FROM trains INNER JOIN trains_stations ON trains.id_train=trains_stations.id_train "
			+ "AND travel_time=0) AS t1 INNER JOIN trains_stations ON t1.id_train=trains_stations.id_train AND stay_time=-1 "
			+ "ORDER BY t1.id_train LIMIT ? OFFSET ?";

	public static final Map<Integer, Train> TRAINS_MAP = new HashMap<Integer, Train>();

	public static void loadTrainsCache() {
		int count = Integer.MAX_VALUE;
		int offset = 0;
		while (count >= BATCH_SIZE) {
			try (Connection con = DBConnection.getDbConnection();
					PreparedStatement ps = con.prepareStatement(TRAINS_SQL)) {
				ps.setInt(1, BATCH_SIZE);
				ps.setInt(2, offset);
				ResultSet rs = ps.executeQuery();
				count = 0;
				while (rs.next()) {
					++count;
					int idTrain = rs.getInt(1);
					String name = rs.getString(2);
					name = name == null ? null : name.trim();
					name = name != null && name.isEmpty() ? null : name;
					Date departureTime = rs.getTimestamp(3);
					String departureDays = rs.getString(4);
					int departureStationId = rs.getInt(5);
					int destinationStationId = rs.getInt(6);
					String departureStation = StationDao.STATIONS_ID_NAME_MAP.get(departureStationId);
					String destinationStation = StationDao.STATIONS_ID_NAME_MAP.get(destinationStationId);
					Train train = new Train(name, departureTime, departureDays, departureStation, destinationStation);
					TRAINS_MAP.put(idTrain, train);
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

	public static int getTravelStayTime(int idTrain, int idStation) {
		int travelStayTime = -1;
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = DBConnection.getDbConnection();
			con.setAutoCommit(false);
			String sql = "SELECT travel_time+stay_time FROM trains_stations WHERE id_train=? AND id_station=?";
			ps = con.prepareStatement(sql);
			ps.setInt(1, idTrain);
			ps.setInt(2, idStation);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				travelStayTime = rs.getInt(1);
			}
			rs.close();
			con.commit();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (con != null)
					con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return travelStayTime;
	}

	public static int getTravelTime(int idTrain, int idStation) {
		int travelTime = -1;
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = DBConnection.getDbConnection();
			con.setAutoCommit(false);
			String sql = "SELECT travel_time FROM trains_stations WHERE id_train=? AND id_station=?";
			ps = con.prepareStatement(sql);
			ps.setInt(1, idTrain);
			ps.setInt(2, idStation);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				travelTime = rs.getInt(1);
			}
			rs.close();
			con.commit();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (con != null)
					con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return travelTime;
	}

	// на все дни
	public static List<TrainTravelStayTimes> getTrainsByStations(int idDepartureStation, int idDestinationStation) {
		List<TrainTravelStayTimes> result = new LinkedList<TrainTravelStayTimes>();
		try (Connection con = DBConnection.getDbConnection();
				PreparedStatement ps = con.prepareStatement(TRAINS_ALL_DAYS_SQL)) {
			ps.setInt(1, idDepartureStation);
			ps.setInt(2, idDestinationStation);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				int idTrain = rs.getInt(1);
				int departureTravelStayTime = rs.getInt(2);
				int destinationTravelTime = rs.getInt(3);
				result.add(new TrainTravelStayTimes(idTrain, departureTravelStayTime, destinationTravelTime));
			}
			rs.close();
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
		return result;
	}

	// на заданный день
	public static List<TrainTravelStayTimes> getTrainsByStationsAndDate(int idDepartureStation,
			int idDestinationStation, String dateStr) {
		List<TrainTravelStayTimes> result = new LinkedList<TrainTravelStayTimes>();
		try (Connection con = DBConnection.getDbConnection();
				PreparedStatement ps = con.prepareStatement(TRAINS_DATE_SQL)) {
			ps.setInt(1, idDepartureStation);
			ps.setInt(2, idDestinationStation);
			ps.setString(3, dateStr);
			ps.setString(4, dateStr);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				int idTrain = rs.getInt(1);
				int departureTravelStayTime = rs.getInt(2);
				int destinationTravelTime = rs.getInt(3);
				result.add(new TrainTravelStayTimes(idTrain, departureTravelStayTime, destinationTravelTime));
			}
			rs.close();
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
		return result;
	}

}
