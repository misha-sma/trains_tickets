package rzd.persistence.entity;

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
    String passportStr=params.get("passport");
    long passport=Long.parseLong(passportStr);
    String surname=params.get("surname");
    String name=params.get("name");
    String patronymic=params.get("patronymic");
    String birthdayStr=params.get("birthday");
    String pattern = "yyyy-MM-dd";
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
    Date birthday = simpleDateFormat.parse(birthdayStr);
    String phoneStr=params.get("phone").replace("+", "");
    long phone=Long.parseLong(phoneStr);
    String email=params.get("email");
  }
}
