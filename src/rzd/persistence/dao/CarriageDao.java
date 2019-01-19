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

import rzd.persistence.DBConnection;
import rzd.persistence.entity.Carriage;

public class CarriageDao {
	public static List<Carriage> getCarriages() {
		List<Carriage> carriages = new LinkedList<Carriage>();
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = DBConnection.getDbConnection();
			con.setAutoCommit(false);
			String sql = "SELECT * from carriages ORDER BY id_carriage";
			ps = con.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				long idCarriage = rs.getLong(1);
				int idTrain = rs.getInt(2);
				Date departureTime = new Date(rs.getTimestamp(3).getTime());
				int carriageNumber = rs.getInt(4);
				int idCarriageType = rs.getInt(5);
				carriages.add(new Carriage(idCarriage, idTrain, departureTime, carriageNumber, idCarriageType));
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
		return carriages;
	}

	public static Map<Integer, Integer> getSeatsCountMap() {
		Map<Integer, Integer> result = new HashMap<Integer, Integer>();
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = DBConnection.getDbConnection();
			con.setAutoCommit(false);
			String sql = "SELECT id_carriage_type, seats_count FROM carriage_types";
			ps = con.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				int idCarriageType = rs.getInt(1);
				int seatsCount = rs.getInt(2);
				result.put(idCarriageType, seatsCount);
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
		return result;
	}

	public static Map<Integer, String> getCarriageNamesMap() {
		Map<Integer, String> result = new HashMap<Integer, String>();
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = DBConnection.getDbConnection();
			con.setAutoCommit(false);
			String sql = "SELECT id_carriage_type, name FROM carriage_types";
			ps = con.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				int idCarriageType = rs.getInt(1);
				String name = rs.getString(2);
				result.put(idCarriageType, name);
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
		return result;
	}
}
