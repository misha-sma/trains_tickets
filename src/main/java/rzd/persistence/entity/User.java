package rzd.persistence.entity;

import java.util.Date;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rzd.util.DateUtil;

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
		surname = firstUp(params.get("surname"));
		name = firstUp(params.get("name"));
		patronymic = firstUp(params.get("patronymic"));
		String birthdayStr = params.get("birthday");
		birthday = DateUtil.string2Date(birthdayStr);
		String phoneStr = params.get("phone").replace("+", "");
		phone = Long.parseLong(phoneStr);
		email = params.get("email");
	}

	private String firstUp(String str) {
		if (str == null || str.isEmpty()) {
			return str;
		}
		return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
	}

	public boolean isMatching(User user) {
		return surname.equalsIgnoreCase(user.getSurname()) && name.equalsIgnoreCase(user.getName())
				&& patronymic.equalsIgnoreCase(user.getPatronymic()) && birthday.equals(user.getBirthday())
				&& phone == user.getPhone() && email.equalsIgnoreCase(user.getEmail());
	}
}
