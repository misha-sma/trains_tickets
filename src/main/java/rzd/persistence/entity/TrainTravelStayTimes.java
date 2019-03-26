package rzd.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TrainTravelStayTimes {
	private int idTrain;
	private int departureTravelStayTime;
	private int destinationTravelTime;
}
