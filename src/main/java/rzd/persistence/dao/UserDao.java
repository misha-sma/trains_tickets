package rzd.persistence.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rzd.persistence.DBConnection;
import rzd.persistence.entity.User;

public class UserDao {
	private static final Logger logger = LoggerFactory.getLogger(UserDao.class);

	public static final String USER_BY_ID_SQL = "SELECT * FROM users WHERE id_user=?";
	public static final String ADD_USER_SQL = "INSERT INTO users (id_user, surname, name, patronymic, birthday, phone, email) VALUES (?, ?, ?, ?, ?, ?, ?)";
	public static final String UPDATE_USER_SQL = "UPDATE users SET surname=?, name=?, patronymic=?, birthday=?, phone=?, email=? WHERE id_user=?";

	public static User getUserById(long idUser) {
		User user = null;
		try (Connection con = DBConnection.getDbConnection();
				PreparedStatement ps = con.prepareStatement(USER_BY_ID_SQL)) {
			ps.setLong(1, idUser);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				String surname = rs.getString("surname");
				String name = rs.getString("name");
				String patronymic = rs.getString("patronymic");
				Date birthday = rs.getTimestamp("birthday");
				long phone = rs.getLong("phone");
				String email = rs.getString("email");
				Date registrationDate = rs.getTimestamp("registration_date");
				user = new User(idUser, surname, name, patronymic, birthday, phone, email, registrationDate);
			}
			rs.close();
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
		return user;
	}

	public static void addUser(User user) {
		try (Connection con = DBConnection.getDbConnection();
				PreparedStatement ps = con.prepareStatement(ADD_USER_SQL)) {
			ps.setLong(1, user.getIdUser());
			ps.setString(2, user.getSurname());
			ps.setString(3, user.getName());
			ps.setString(4, user.getPatronymic());
			ps.setDate(5, new java.sql.Date(user.getBirthday().getTime()));
			ps.setLong(6, user.getPhone());
			ps.setString(7, user.getEmail());
			ps.execute();
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public static void updateUser(User user) {
		try (Connection con = DBConnection.getDbConnection();
				PreparedStatement ps = con.prepareStatement(UPDATE_USER_SQL)) {
			ps.setString(1, user.getSurname());
			ps.setString(2, user.getName());
			ps.setString(3, user.getPatronymic());
			ps.setDate(4, new java.sql.Date(user.getBirthday().getTime()));
			ps.setLong(5, user.getPhone());
			ps.setString(6, user.getEmail());
			ps.setLong(7, user.getIdUser());
			ps.execute();
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
	}

}
