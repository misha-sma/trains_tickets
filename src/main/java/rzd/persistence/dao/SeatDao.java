package rzd.persistence.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rzd.persistence.DBConnection;
import rzd.persistence.entity.Carriage;
import rzd.persistence.entity.CarriageSeatNumber;
import rzd.persistence.entity.SeatsSearchResult;
import rzd.util.Util;

public class SeatDao {
	private static final Logger logger = LoggerFactory.getLogger(SeatDao.class);

	public static final int SEATS_HASH_BASE = 1000;
	public static final String PREVIOUS_STATIONS_COUNT_SQL = "SELECT count(1) FROM trains_stations WHERE id_train=? AND travel_time<="
			+ "(SELECT travel_time FROM trains_stations WHERE id_train=? AND id_station=?)";
	public static final String CARRIAGE_BY_ID_SEAT_SQL = "SELECT seat_number, carriages.* FROM seats INNER JOIN carriages ON "
			+ "seats.id_carriage=carriages.id_carriage AND id_seat=?";

	public static void addOneTrainSeats(List<Carriage> carriages, int stagesCount) {
		String sql = "INSERT INTO seats (id_carriage, seat_number, stages) VALUES (?, ?, B'"
				+ Util.getZerosString(stagesCount) + "')";
		try (Connection con = DBConnection.getDbConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
			for (Carriage carriage : carriages) {
				for (int seatNumber = 1; seatNumber <= CarriageDao.SEATS_COUNT_MAP
						.get(carriage.getIdCarriageType()); ++seatNumber) {
					ps.setLong(1, carriage.getIdCarriage());
					ps.setInt(2, seatNumber);
					ps.addBatch();
				}
			}
			ps.executeBatch();
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
	}

	private static int getPreviousStationsCount(int idTrain, int idStation) {
		int count = 0;
		try (Connection con = DBConnection.getDbConnection();
				PreparedStatement ps = con.prepareStatement(PREVIOUS_STATIONS_COUNT_SQL)) {
			ps.setInt(1, idTrain);
			ps.setInt(2, idTrain);
			ps.setInt(3, idStation);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				count = rs.getInt(1);
			}
			rs.close();
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
		return count;
	}

	public static SeatsSearchResult getFreeSeats(int idTrain, String departureDate, int idDepartureStation,
			int idDestinationStation, int delay) {
		int maxCarriageNumber = 0;
		Map<Integer, Integer> carriageTypesMap = new HashMap<Integer, Integer>();
		Map<Integer, Long> seatsMap = new HashMap<Integer, Long>();
		int num1 = getPreviousStationsCount(idTrain, idDepartureStation);
		int num2 = getPreviousStationsCount(idTrain, idDestinationStation);
		String sql = "SELECT id_seat, seat_number, carriage_number, id_carriage_type FROM seats INNER JOIN carriages "
				+ "ON seats.id_carriage=carriages.id_carriage WHERE id_train=? AND departure_time+"
				+ "?*interval '1 minute'>='" + departureDate + " 00:00:00' AND departure_time+?*interval '1 minute'<='"
				+ departureDate + " 23:59:59' AND substring(stages, ?, ?)=B'" + Util.getZerosString(num2 - num1) + "'";
		try (Connection con = DBConnection.getDbConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setInt(1, idTrain);
			ps.setInt(2, delay);
			ps.setInt(3, delay);
			ps.setInt(4, num1);
			ps.setInt(5, num2 - num1);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				long idSeat = rs.getLong(1);
				int seatNumber = rs.getInt(2);
				int carriageNumber = rs.getInt(3);
				int carriageType = rs.getInt(4);
				if (carriageNumber > maxCarriageNumber) {
					maxCarriageNumber = carriageNumber;
				}
				carriageTypesMap.put(carriageNumber, carriageType);
				seatsMap.put(carriageNumber * SEATS_HASH_BASE + seatNumber, idSeat);
			}
			rs.close();
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
		return new SeatsSearchResult(maxCarriageNumber, carriageTypesMap, seatsMap);
	}

	public static void updateSeat(long idSeat, int idDepartureStation, int idDestinationStation, int idTrain) {
		int num1 = getPreviousStationsCount(idTrain, idDepartureStation);
		int num2 = getPreviousStationsCount(idTrain, idDestinationStation);
		String sql = "UPDATE seats SET stages=overlay(stages placing B'" + Util.getOnesString(num2 - num1)
				+ "' from ?) WHERE id_seat=?";
		try (Connection con = DBConnection.getDbConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setInt(1, num1);
			ps.setLong(2, idSeat);
			ps.execute();
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public static CarriageSeatNumber getCarriageSeatNumberByIdSeat(long idSeat) {
		CarriageSeatNumber carriageSN = null;
		try (Connection con = DBConnection.getDbConnection();
				PreparedStatement ps = con.prepareStatement(CARRIAGE_BY_ID_SEAT_SQL)) {
			ps.setLong(1, idSeat);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				int seatNumber = rs.getInt(1);
				long idCarriage = rs.getLong(2);
				int idTrain = rs.getInt(3);
				Date departureDate = rs.getTimestamp(4);
				int carriageNumber = rs.getInt(5);
				int idCarriageType = rs.getInt(6);
				carriageSN = new CarriageSeatNumber(
						new Carriage(idCarriage, idTrain, departureDate, carriageNumber, idCarriageType), seatNumber);
			}
			rs.close();
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
		return carriageSN;
	}

}
