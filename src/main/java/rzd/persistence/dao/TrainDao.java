package rzd.persistence.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import rzd.persistence.DBConnection;
import rzd.persistence.entity.Train;

public class TrainDao {

  public static int getTravelStayTime(int idTrain, int idStation) {
    int travelStayTime = -1;
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = DBConnection.getDbConnection();
      con.setAutoCommit(false);
      String sql = "SELECT travel_time+stay_time FROM trains_stations WHERE id_train=? AND id_station=?";
      ps = con.prepareStatement(sql);
      ps.setInt(1, idTrain);
      ps.setInt(2, idStation);
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        travelStayTime = rs.getInt(1);
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
    return travelStayTime;
  }
  
  public static int getTravelTime(int idTrain, int idStation) {
    int travelTime = -1;
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = DBConnection.getDbConnection();
      con.setAutoCommit(false);
      String sql = "SELECT travel_time FROM trains_stations WHERE id_train=? AND id_station=?";
      ps = con.prepareStatement(sql);
      ps.setInt(1, idTrain);
      ps.setInt(2, idStation);
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        travelTime = rs.getInt(1);
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
    return travelTime;
  }

  public static Train getTrainById(int idTrain) {
    Train train = new Train();
    train.setIdTrain(idTrain);
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = DBConnection.getDbConnection();
      con.setAutoCommit(false);
      String sql = "SELECT * FROM trains WHERE id_train=?";
      ps = con.prepareStatement(sql);
      ps.setInt(1, idTrain);
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        train.setName(rs.getString(2));
        train.setDepartureTime(new Date(rs.getTimestamp(3).getTime()));
        train.setDepartureDays(rs.getString(4));
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
    return train;
  }

  public static String getDepartureStation(int idTrain) {
    String stationName = null;
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = DBConnection.getDbConnection();
      con.setAutoCommit(false);
      String sql =
          "SELECT name FROM stations WHERE id_station=(SELECT id_station FROM trains_stations WHERE id_train=? AND travel_time=0)";
      ps = con.prepareStatement(sql);
      ps.setInt(1, idTrain);
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        stationName = rs.getString(1);
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
    return stationName;
  }

  public static String getDestinationStation(int idTrain) {
    String stationName = null;
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = DBConnection.getDbConnection();
      con.setAutoCommit(false);
      String sql =
          "SELECT name FROM stations WHERE id_station=(SELECT id_station FROM trains_stations WHERE id_train=? ORDER BY travel_time DESC LIMIT 1)";
      ps = con.prepareStatement(sql);
      ps.setInt(1, idTrain);
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        stationName = rs.getString(1);
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
    return stationName;
  }
  
	// на все дни
	public static List<Integer> getTrainsByStations(int idDepartureStation, int idDestinationStation) {
		List<Integer> result = new LinkedList<Integer>();
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = DBConnection.getDbConnection();
			con.setAutoCommit(false);
			String sql = "SELECT t1.id_train FROM (SELECT * FROM trains_stations WHERE id_station=?) AS t1 INNER JOIN "
					+ "(SELECT * FROM trains_stations WHERE id_station=?) AS t2 ON "
					+ "(t1.id_train=t2.id_train AND t1.travel_time<t2.travel_time) ORDER BY t1.id_train";
			ps = con.prepareStatement(sql);
			ps.setInt(1, idDepartureStation);
			ps.setInt(2, idDestinationStation);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				int idTrain = rs.getInt(1);
				result.add(idTrain);
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

	// на заданный день
	public static List<Integer> getTrainsByStationsAndDate(int idDepartureStation, int idDestinationStation,
	    String dateStr) {
//		Calendar cal = Calendar.getInstance();
//		cal.setTime(date);
//		int year = cal.get(Calendar.YEAR);
//		int month = cal.get(Calendar.MONTH) + 1;
//		int day = cal.get(Calendar.DAY_OF_MONTH);
//		String dateStr = String.valueOf(year) + "-" + month + "-" + day;
		List<Integer> result = new LinkedList<Integer>();
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = DBConnection.getDbConnection();
			con.setAutoCommit(false);
			String sql = "SELECT DISTINCT carriages.id_train FROM carriages INNER JOIN (SELECT t1.id_train, t1.travel_time, t1.stay_time "
					+ "FROM (SELECT * FROM trains_stations WHERE id_station=?) AS t1 INNER JOIN (SELECT * FROM trains_stations WHERE "
					+ "id_station=?) AS t2 ON (t1.id_train=t2.id_train AND t1.travel_time<t2.travel_time)) AS t3 ON "
					+ "(carriages.id_train=t3.id_train AND departure_time+(t3.travel_time+t3.stay_time)*interval '1 minute'>='"
					+ dateStr + " 00:00:00' AND departure_time+(t3.travel_time+t3.stay_time)*interval '1 minute'<='"
					+ dateStr + " 23:59:59') ORDER BY carriages.id_train";
			ps = con.prepareStatement(sql);
			ps.setInt(1, idDepartureStation);
			ps.setInt(2, idDestinationStation);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				int idTrain = rs.getInt(1);
				result.add(idTrain);
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
