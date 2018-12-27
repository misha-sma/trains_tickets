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
public class Carriage {
	private long idCarriage;
	private int idTrain;
	private Date departureTime;
	private int carriageNumber;
	private int idCarriageType;
}
