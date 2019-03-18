package rzd.persistence.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import rzd.persistence.DBConnection;

public class StationDao {
  public static String getStationNameById(int idStation) {
    String name = null;
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = DBConnection.getDbConnection();
      con.setAutoCommit(false);
      String sql = "SELECT name FROM stations WHERE id_station=?";
      ps = con.prepareStatement(sql);
      ps.setInt(1, idStation);
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        name = rs.getString(1);
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
    return name;
  }
}
