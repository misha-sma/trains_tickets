package rzd.persistence.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import rzd.MainClass;
import rzd.persistence.DBConnection;
import rzd.persistence.entity.Carriage;

public class SeatDao {

	public static void addOneCarriageSeats(Carriage carriage) {
		for (int seatNumber = 1; seatNumber <= MainClass.SEATS_COUNT_MAP
				.get(carriage.getIdCarriageType()); ++seatNumber) {
			addOneSeat(carriage, seatNumber);
		}
	}

	public static void addOneSeat(Carriage carriage, int seatNumber) {
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = DBConnection.getDbConnection();
			con.setAutoCommit(false);
			String sql = "INSERT INTO seats (id_carriage, seat_number) VALUES (?, ?)";
			ps = con.prepareStatement(sql);
			ps.setLong(1, carriage.getIdCarriage());
			ps.setInt(2, seatNumber);
			ps.execute();
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
	}

	private static int getPreviousStationsCount(int idTrain, int idStation) {
		int count = 0;
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = DBConnection.getDbConnection();
			con.setAutoCommit(false);
			String sql = "SELECT count(1) FROM trains_stations WHERE id_train=? AND travel_time<="
					+ "(SELECT travel_time FROM trains_stations WHERE id_train=? AND id_station=?)";
			ps = con.prepareStatement(sql);
			ps.setInt(1, idTrain);
			ps.setInt(2, idTrain);
			ps.setInt(3, idStation);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				count = rs.getInt(1);
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
		return count;
	}

	private static String getCondition(int idTrain, int idDepartureStation, int idDestinationStation) {
		int num1 = getPreviousStationsCount(idTrain, idDepartureStation);
		int num2 = getPreviousStationsCount(idTrain, idDestinationStation);
		StringBuilder builder = new StringBuilder();
		for (int i = num1; i < num2; ++i) {
			if (i > num1) {
				builder.append("AND ");
			}
			builder.append("stage_" + i + "='f' ");
		}
		return builder.toString();
	}

	public static void getFreeSeats(int idTrain, Date departureDate, int idDepartureStation, int idDestinationStation) {
		String condition = getCondition(idTrain, idDepartureStation, idDestinationStation);
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = DBConnection.getDbConnection();
			con.setAutoCommit(false);
			String sql = "SELECT * FROM seats INNER JOIN (SELECT * FROM carriages WHERE id_train=? AND departure_time=?) AS t1 "
					+ "ON seats.id_carriage=t1.id_carriage WHERE " + condition;
			ps = con.prepareStatement(sql);
			ps.setInt(1, idTrain);
			ps.setInt(2, idTrain);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
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
	}
}
