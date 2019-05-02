package rzd.persistence.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rzd.persistence.DBConnection;
import rzd.persistence.entity.Train;
import rzd.persistence.entity.TrainTravelStayTimes;
import rzd.util.DateUtil;

public class TrainDao {
	private static final Logger logger = LoggerFactory.getLogger(TrainDao.class);

	public static final int BATCH_SIZE = 1000;
	public static final String TRAINS_SQL = "SELECT t1.*, trains_stations.id_station FROM "
			+ "(SELECT trains.*, trains_stations.id_station FROM trains INNER JOIN trains_stations ON trains.id_train=trains_stations.id_train "
			+ "AND travel_time=0) AS t1 INNER JOIN trains_stations ON t1.id_train=trains_stations.id_train AND stay_time=-1 "
			+ "ORDER BY t1.id_train LIMIT ? OFFSET ?";
	public static final Map<Integer, Train> TRAINS_MAP = new HashMap<Integer, Train>();

	public static final String TRAINS_ALL_DAYS_SQL = "SELECT t1.id_train, t1.travel_time+t1.stay_time, t2.travel_time FROM trains_stations "
			+ "AS t1 INNER JOIN trains_stations AS t2 ON t1.id_station=? AND t2.id_station=? AND t1.id_train=t2.id_train "
			+ "AND t1.travel_time<t2.travel_time ORDER BY t1.id_train";

	public static final String TRAVEL_STAY_TIME_SQL = "SELECT travel_time+stay_time FROM trains_stations WHERE id_train=? AND id_station=?";
	public static final String TRAVEL_TIME_SQL = "SELECT travel_time FROM trains_stations WHERE id_train=? AND id_station=?";

	public static final String ADD_TRAIN_STATION_SQL = "INSERT INTO trains_stations (id_train, id_station, travel_time, stay_time) "
			+ "VALUES (?, ?, ?, ?)";

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
		try (Connection con = DBConnection.getDbConnection();
				PreparedStatement ps = con.prepareStatement(TRAVEL_STAY_TIME_SQL)) {
			ps.setInt(1, idTrain);
			ps.setInt(2, idStation);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				travelStayTime = rs.getInt(1);
			}
			rs.close();
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
		return travelStayTime;
	}

	public static int getTravelTime(int idTrain, int idStation) {
		int travelTime = -1;
		try (Connection con = DBConnection.getDbConnection();
				PreparedStatement ps = con.prepareStatement(TRAVEL_TIME_SQL)) {
			ps.setInt(1, idTrain);
			ps.setInt(2, idStation);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				travelTime = rs.getInt(1);
			}
			rs.close();
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
		return travelTime;
	}

	// на все дни
	public static List<TrainTravelStayTimes> getTrainsByStations(int idDepartureStation, int idDestinationStation) {
		List<TrainTravelStayTimes> result = new ArrayList<TrainTravelStayTimes>();
		try (Connection con = DBConnection.getDbConnection();
				PreparedStatement ps = con.prepareStatement(TRAINS_ALL_DAYS_SQL)) {
			ps.setInt(1, idDepartureStation);
			ps.setInt(2, idDestinationStation);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				int idTrain = rs.getInt(1);
				int departureTravelStayTime = rs.getInt(2);
				int destinationTravelTime = rs.getInt(3);
				String departureTime = DateUtil.addMinutesToDate(TRAINS_MAP.get(idTrain).getDepartureTime(),
						departureTravelStayTime);
				result.add(new TrainTravelStayTimes(idTrain, departureTravelStayTime, destinationTravelTime,
						departureTime));
			}
			rs.close();
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
		Collections.sort(result);
		return result;
	}

	public static void addTrain(int idTrain, String name, String departureTime, String departureDays) {
		String sql = "INSERT INTO trains (id_train, name, departure_time, departure_days) " + "VALUES (?, ?, '"
				+ departureTime + "', ?)";
		try (Connection con = DBConnection.getDbConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setInt(1, idTrain);
			ps.setString(2, name);
			ps.setString(3, departureDays);
			ps.execute();
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public static void addTrainStation(int idTrain, int idStation, int travelTime, int stayTime) {
		try (Connection con = DBConnection.getDbConnection();
				PreparedStatement ps = con.prepareStatement(ADD_TRAIN_STATION_SQL)) {
			ps.setInt(1, idTrain);
			ps.setInt(2, idStation);
			ps.setInt(3, travelTime);
			ps.setInt(4, stayTime);
			ps.execute();
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
	}

}
