package rzd.persistence.entity;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Train {
	private String name;
	private Date departureTime;
	private String departureDays;
	private String departureStation;
	private String destinationStation;
	private int stagesCount;
}
