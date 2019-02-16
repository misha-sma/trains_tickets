package rzd.persistence.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import rzd.persistence.DBConnection;

public class TicketDao {
	public static void createTicket(long idSeat, int idDepartureStation, int idDestinationStation, long idUser) {
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = DBConnection.getDbConnection();
			con.setAutoCommit(false);
			String sql = "INSERT INTO tickets (id_seat, departure_station, destination_station, id_user) VALUES (?, ?, ?, ?)";
			ps = con.prepareStatement(sql);
			ps.setLong(1, idSeat);
			ps.setInt(2, idDepartureStation);
			ps.setInt(3, idDestinationStation);
			ps.setLong(4, idUser);
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
}
