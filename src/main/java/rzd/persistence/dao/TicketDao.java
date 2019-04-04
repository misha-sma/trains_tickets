package rzd.persistence.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rzd.persistence.DBConnection;

public class TicketDao {
	private static final Logger logger = LoggerFactory.getLogger(TicketDao.class);

	public static final String ADD_TICKET_SQL = "INSERT INTO tickets (id_seat, departure_station, destination_station, id_user) VALUES (?, ?, ?, ?)";

	public static void createTicket(long idSeat, int idDepartureStation, int idDestinationStation, long idUser) {
		try (Connection con = DBConnection.getDbConnection();
				PreparedStatement ps = con.prepareStatement(ADD_TICKET_SQL)) {
			ps.setLong(1, idSeat);
			ps.setInt(2, idDepartureStation);
			ps.setInt(3, idDestinationStation);
			ps.setLong(4, idUser);
			ps.execute();
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
	}
}
