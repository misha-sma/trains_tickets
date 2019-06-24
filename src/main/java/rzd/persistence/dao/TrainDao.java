package rzd.persistence.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rzd.persistence.DBConnection;
import rzd.persistence.entity.TimeTable;
import rzd.persistence.entity.Train;
import rzd.persistence.entity.TrainTravelStayTimes;
import rzd.persistence.entity.TrainTravelStayTimesOneTransfer;
import rzd.util.DateUtil;

public class TrainDao {
	private static final Logger logger = LoggerFactory.getLogger(TrainDao.class);

	public static final int BATCH_SIZE = 1000;
	public static final String TRAINS_SQL = "SELECT t2.*, t3.stations_count FROM (SELECT t1.*, trains_stations.id_station FROM "
			+ "(SELECT trains.*, trains_stations.id_station FROM trains INNER JOIN trains_stations ON trains.id_train=trains_stations.id_train "
			+ "AND travel_time=0) AS t1 INNER JOIN trains_stations ON t1.id_train=trains_stations.id_train AND stay_time=-1) "
			+ "AS t2 INNER JOIN (SELECT id_train, count(1) AS stations_count FROM trains_stations GROUP BY id_train) AS t3 ON t2.id_train=t3.id_train "
			+ "ORDER BY t2.id_train LIMIT ? OFFSET ?";
	public static final Map<Integer, Train> TRAINS_MAP = new HashMap<Integer, Train>();

	public static final String TRAINS_ALL_DAYS_SQL = "SELECT t1.id_train, t1.travel_time+t1.stay_time, t2.travel_time FROM trains_stations "
			+ "AS t1 INNER JOIN trains_stations AS t2 ON t1.id_station=? AND t2.id_station=? AND t1.id_train=t2.id_train "
			+ "AND t1.travel_time<t2.travel_time";

	public static final String TRAINS_ALL_DAYS_ONE_TRANSFER_SQL = "WITH tmp_table AS ("
			+ "SELECT t2.id_train AS id_train_from, t2.id_station, t4.id_train AS id_train_to, t2.travel_stay_time_from, t2.travel_time_from, t4.travel_stay_time_to, t4.travel_time_to FROM "
			+ "(SELECT trains_stations.id_train, trains_stations.id_station, trains_stations.travel_time AS travel_time_from, t1.travel_stay_time_from FROM trains_stations INNER JOIN "
			+ "(SELECT id_train, travel_time, (travel_time+stay_time) AS travel_stay_time_from FROM trains_stations WHERE id_station=?) AS t1 "
			+ "ON trains_stations.id_train=t1.id_train AND trains_stations.travel_time>t1.travel_time" + ") AS t2 "
			+ "INNER JOIN "
			+ "(SELECT trains_stations.id_train, trains_stations.id_station, (trains_stations.travel_time+trains_stations.stay_time) AS travel_stay_time_to, t3.travel_time AS travel_time_to FROM trains_stations INNER JOIN "
			+ "(SELECT id_train, travel_time FROM trains_stations WHERE id_station=?) AS t3 "
			+ "ON trains_stations.id_train=t3.id_train AND trains_stations.travel_time<t3.travel_time" + ") AS t4 "
			+ "ON t2.id_station=t4.id_station" + ") " + "SELECT tmp_table.* FROM tmp_table INNER JOIN "
			+ "(SELECT id_train_from, min(travel_time_from) AS min_time, id_train_to FROM tmp_table GROUP BY id_train_from, id_train_to) AS t5 "
			+ "ON tmp_table.id_train_from=t5.id_train_from AND tmp_table.id_train_to=t5.id_train_to AND t5.min_time=tmp_table.travel_time_from";

	public static final String TRAVEL_STAY_TIME_SQL = "SELECT travel_time+stay_time FROM trains_stations WHERE id_train=? AND id_station=?";
	public static final String TRAVEL_TIME_SQL = "SELECT travel_time FROM trains_stations WHERE id_train=? AND id_station=?";
	public static final String TIME_TABLE_SQL = "SELECT id_station, travel_time, stay_time FROM trains_stations WHERE id_train=? ORDER BY travel_time";

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
					int stagesCount = rs.getInt(7) - 1;
					String departureStation = StationDao.STATIONS_ID_NAME_MAP.get(departureStationId);
					String destinationStation = StationDao.STATIONS_ID_NAME_MAP.get(destinationStationId);
					Train train = new Train(name, departureTime, departureDays, departureStation, destinationStation, stagesCount);
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

	public static List<TimeTable> getTimeTable(int idTrain) {
		List<TimeTable> timeTable = new LinkedList<TimeTable>();
		try (Connection con = DBConnection.getDbConnection();
				PreparedStatement ps = con.prepareStatement(TIME_TABLE_SQL)) {
			ps.setInt(1, idTrain);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				int idStation = rs.getInt(1);
				int travelTime = rs.getInt(2);
				int stayTime = rs.getInt(3);
				timeTable.add(new TimeTable(idStation, travelTime, stayTime));
			}
			rs.close();
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
		return timeTable;
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

	// на все дни c одной пересадкой
	public static List<TrainTravelStayTimesOneTransfer> getTrainsByStationsOneTransfer(int idDepartureStation,
			int idDestinationStation) {
		List<TrainTravelStayTimesOneTransfer> result = new ArrayList<TrainTravelStayTimesOneTransfer>();
		try (Connection con = DBConnection.getDbConnection();
				PreparedStatement ps = con.prepareStatement(TRAINS_ALL_DAYS_ONE_TRANSFER_SQL)) {
			ps.setInt(1, idDepartureStation);
			ps.setInt(2, idDestinationStation);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				int idTrainFrom = rs.getInt(1);
				int transferIdStation = rs.getInt(2);
				int idTrainTo = rs.getInt(3);
				int travelStayTimeFrom = rs.getInt(4);
				int travelTimeFrom = rs.getInt(5);
				int travelStayTimeTo = rs.getInt(6);
				int travelTimeTo = rs.getInt(7);
				result.add(new TrainTravelStayTimesOneTransfer(idTrainFrom, transferIdStation, idTrainTo,
						travelStayTimeFrom, travelTimeFrom, travelStayTimeTo, travelTimeTo));
			}
			rs.close();
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
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
