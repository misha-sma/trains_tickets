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
	private int idTrain;
	private String name;
	private Date departureTime;
	private String departureDays;
}
