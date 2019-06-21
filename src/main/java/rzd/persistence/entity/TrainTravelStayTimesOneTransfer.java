package rzd.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TrainTravelStayTimesOneTransfer {
	private int idTrainFrom;
	private int transferIdStation;
	private int idTrainTo;
	private int travelStayTimeFrom;
	private int travelTimeFrom;
	private int travelStayTimeTo;
	private int travelTimeTo;
}
