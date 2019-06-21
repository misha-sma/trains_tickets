package rzd.persistence.entity;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StayTimeWithDepartureDays {
	private int stayTimeMinutes;
	private List<String> departureDays;
}
