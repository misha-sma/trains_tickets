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
import rzd.persistence.entity.Carriage;

public class CarriageDao {
	private static final Logger logger = LoggerFactory.getLogger(CarriageDao.class);
	
	public static final Map<Integer, Integer> SEATS_COUNT_MAP = new HashMap<Integer, Integer>();
	public static final Map<Integer, String> CARRIAGE_NAMES_MAP = new HashMap<Integer, String>();

	public static final String CARRIAGE_TYPES_SQL = "SELECT * FROM carriage_types";
	
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

  public static Carriage getCarriageByIdSeat(long idSeat) {
    Carriage carriage = null;
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = DBConnection.getDbConnection();
      con.setAutoCommit(false);
      String sql = "SELECT * from carriages WHERE id_carriage=(SELECT id_carriage FROM seats WHERE id_seat=?)";
      ps = con.prepareStatement(sql);
      ps.setLong(1, idSeat);
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        long idCarriage = rs.getLong(1);
        int idTrain = rs.getInt(2);
        Date departureTime = new Date(rs.getTimestamp(3).getTime());
        int carriageNumber = rs.getInt(4);
        int idCarriageType = rs.getInt(5);
        carriage = new Carriage(idCarriage, idTrain, departureTime, carriageNumber, idCarriageType);
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
    return carriage;
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
