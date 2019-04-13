package rzd.persistence.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rzd.persistence.DBConnection;
import rzd.persistence.entity.Carriage;

public class CarriageDao {
	private static final Logger logger = LoggerFactory.getLogger(CarriageDao.class);

	public static final Map<Integer, Integer> SEATS_COUNT_MAP = new HashMap<Integer, Integer>();
	public static final Map<Integer, String> CARRIAGE_NAMES_MAP = new HashMap<Integer, String>();

	public static final String CARRIAGE_TYPES_SQL = "SELECT * FROM carriage_types";
	public static final String INSERT_CARRIAGE_SQL = "INSERT INTO carriages (id_train, departure_time, carriage_number, id_carriage_type) "
			+ "VALUES (?, ?, ?, ?)";

	public static List<Carriage> getCarriages(int idTrain, String date) {
		List<Carriage> carriages = new ArrayList<Carriage>();
		String sql = "SELECT * FROM carriages WHERE id_train=? AND departure_time>='" + date
				+ " 00:00:00' AND departure_time<='" + date + " 23:59:59' ORDER BY id_carriage";
		try (Connection con = DBConnection.getDbConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setInt(1, idTrain);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				long idCarriage = rs.getLong(1);
				Date departureTime = rs.getTimestamp(3);
				int carriageNumber = rs.getInt(4);
				int idCarriageType = rs.getInt(5);
				carriages.add(new Carriage(idCarriage, idTrain, departureTime, carriageNumber, idCarriageType));
			}
			rs.close();
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
		return carriages;
	}

	public static void saveCarriages(List<Carriage> carriages) {
		try (Connection con = DBConnection.getDbConnection();
				PreparedStatement ps = con.prepareStatement(INSERT_CARRIAGE_SQL, Statement.RETURN_GENERATED_KEYS)) {
			for (Carriage carriage : carriages) {
				ps.setInt(1, carriage.getIdTrain());
				ps.setTimestamp(2, new Timestamp(carriage.getDepartureTime().getTime()));
				ps.setInt(3, carriage.getCarriageNumber());
				ps.setInt(4, carriage.getIdCarriageType());
				ps.addBatch();
			}
			ps.executeBatch();
			ResultSet rs = ps.getGeneratedKeys();
			int i = 0;
			while (rs.next()) {
				long idCarriage = rs.getLong(1);
				carriages.get(i).setIdCarriage(idCarriage);
				++i;
			}
			rs.close();
			if (i != carriages.size()) {
				logger.error("Unsaved carriages");
			}
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public static void loadCarriageCaches() {
		try (Connection con = DBConnection.getDbConnection();
				PreparedStatement ps = con.prepareStatement(CARRIAGE_TYPES_SQL)) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				int idCarriageType = rs.getInt(1);
				String name = rs.getString(2);
				int seatsCount = rs.getInt(3);
				CARRIAGE_NAMES_MAP.put(idCarriageType, name);
				SEATS_COUNT_MAP.put(idCarriageType, seatsCount);
			}
			rs.close();
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
	}

}
