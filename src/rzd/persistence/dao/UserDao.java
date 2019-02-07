package rzd.persistence.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import rzd.persistence.DBConnection;
import rzd.persistence.entity.User;

public class UserDao {
  public static User getUserById(long idUser) {
    User user = null;
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = DBConnection.getDbConnection();
      con.setAutoCommit(false);
      String sql = "SELECT * FROM users WHERE id_user=?";
      ps = con.prepareStatement(sql);
      ps.setLong(1, idUser);
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        String surname = rs.getString("surname");
        String name = rs.getString("name");
        String patronymic = rs.getString("patronymic");
        Date birthday = new Date(rs.getTimestamp("birthday").getTime());
        long phone = rs.getLong("phone");
        String email = rs.getString("email");
        Date registrationDate = new Date(rs.getTimestamp("registration_date").getTime());
        user = new User(idUser, surname, name, patronymic, birthday, phone, email, registrationDate);
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
    return user;
  }

  public static void addUser(long idUser, String surname, String name, String patronymic, Date birthday, long phone,
      String email) {
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = DBConnection.getDbConnection();
      con.setAutoCommit(false);
      String sql =
          "INSERT INTO users (id_user, surname, name, patronymic, birthday, phone, email) VALUES (?, ?, ?, ?, ?, ?, ?)";
      ps = con.prepareStatement(sql);
      ps.setLong(1, idUser);
      ps.setString(2, surname);
      ps.setString(3, name);
      ps.setString(4, patronymic);
      ps.setDate(5, new java.sql.Date(birthday.getTime()));
      ps.setLong(6, phone);
      ps.setString(7, email);
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
