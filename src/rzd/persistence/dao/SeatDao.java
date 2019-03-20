package rzd.persistence.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import rzd.MainClass;
import rzd.persistence.DBConnection;
import rzd.persistence.entity.Carriage;
import rzd.persistence.entity.SeatsSearchResult;
import rzd.persistence.entity.User;
import rzd.util.Util;

public class SeatDao {
public static final int SEATS_HASH_BASE=1000;
	
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

	public static SeatsSearchResult getFreeSeats(int idTrain, String departureDate, int idDepartureStation,
			int idDestinationStation, int delay) {
		int maxCarriageNumber = 0;
		Map<Integer, Integer> carriageTypesMap = new HashMap<Integer, Integer>();
		Map<Integer, Long> seatsMap = new HashMap<Integer, Long>();

		String condition = getCondition(idTrain, idDepartureStation, idDestinationStation);
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = DBConnection.getDbConnection();
			con.setAutoCommit(false);
			String sql = "SELECT id_seat, seat_number, carriage_number, id_carriage_type FROM seats INNER JOIN carriages "
					+ "ON seats.id_carriage=carriages.id_carriage WHERE id_train=? AND departure_time+"
					+ "?*interval '1 minute'>='" + departureDate
					+ " 00:00:00' AND departure_time+?*interval '1 minute'<='" + departureDate + " 23:59:59' AND "
					+ condition;

			// String sql = "SELECT * FROM seats INNER JOIN (SELECT * FROM carriages WHERE
			// id_train=? AND departure_time+"+
			// "?*interval '1 minute'>='"+departureDate+" 00:00:00' AND
			// departure_time+?*interval '1 minute'<='"+departureDate+" 23:59:59') AS t1 "
			// + "ON seats.id_carriage=t1.id_carriage WHERE " + condition;
			ps = con.prepareStatement(sql);
			ps.setInt(1, idTrain);
			ps.setInt(2, delay);
			ps.setInt(3, delay);
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
		return new SeatsSearchResult(maxCarriageNumber, carriageTypesMap, seatsMap);
	}

	private static String getCondition4Update(int idTrain, int idDepartureStation, int idDestinationStation) {
		int num1 = getPreviousStationsCount(idTrain, idDepartureStation);
		int num2 = getPreviousStationsCount(idTrain, idDestinationStation);
		StringBuilder builder = new StringBuilder();
		for (int i = num1; i < num2; ++i) {
			if (i > num1) {
				builder.append(", ");
			}
			builder.append("stage_" + i + "='t' ");
		}
		return builder.toString();
	}

	public static void updateSeat(long idSeat, int idDepartureStation, int idDestinationStation) {
		int idTrain = getIdTrainByIdSeat(idSeat);
	    String condition = getCondition4Update(idTrain, idDepartureStation, idDestinationStation);
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = DBConnection.getDbConnection();
			con.setAutoCommit(false);
			String sql = "UPDATE seats SET " + condition + " WHERE id_seat=?";
			ps = con.prepareStatement(sql);
			ps.setLong(1, idSeat);
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

	public static int getIdTrainByIdSeat(long idSeat) {
      int idTrain = -1;
      Connection con = null;
      PreparedStatement ps = null;
      try {
          con = DBConnection.getDbConnection();
          con.setAutoCommit(false);
          String sql = "SELECT id_train FROM carriages WHERE id_carriage=(SELECT id_carriage FROM seats WHERE id_seat=?)";
          ps = con.prepareStatement(sql);
          ps.setLong(1, idSeat);
          ResultSet rs = ps.executeQuery();
          if (rs.next()) {
              idTrain = rs.getInt(1);
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
      return idTrain;
  }

  public static String getDepartureDateByIdSeat(long idSeat, int idDepartureStation) {
    Date departureDate = null;
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = DBConnection.getDbConnection();
      con.setAutoCommit(false);
      String sql =
          "SELECT departure_time FROM carriages WHERE id_carriage=(SELECT id_carriage FROM seats WHERE id_seat=?)";
      ps = con.prepareStatement(sql);
      ps.setLong(1, idSeat);
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        departureDate = rs.getTimestamp(1);
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
    int idTrain = getIdTrainByIdSeat(idSeat);
    int delay = TrainDao.getTravelStayTime(idTrain, idDepartureStation);
    return Util.addMinutesToDateDate(departureDate, delay);
  }
}
