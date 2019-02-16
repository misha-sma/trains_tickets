package rzd.persistence.entity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class User {
	private long idUser;
	private String surname;
	private String name;
	private String patronymic;
	private Date birthday;
	private long phone;
	private String email;
	private Date registrationDate;

	public User(Map<String, String> params) {
		String passportStr = params.get("passport");
		idUser = Long.parseLong(passportStr);
		surname = params.get("surname");
		name = params.get("name");
		patronymic = params.get("patronymic");
		String birthdayStr = params.get("birthday");
		String pattern = "yyyy-MM-dd";
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		try {
			birthday = simpleDateFormat.parse(birthdayStr);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		String phoneStr = params.get("phone").replace("+", "");
		phone = Long.parseLong(phoneStr);
		email = params.get("email");
	}

	public boolean isMatching(User user) {
		return surname.equalsIgnoreCase(user.getSurname()) && name.equalsIgnoreCase(user.getName())
				&& patronymic.equalsIgnoreCase(user.getPatronymic()) && birthday.equals(user.getBirthday())
				&& phone == user.getPhone() && email.equalsIgnoreCase(user.getEmail());
	}
}
