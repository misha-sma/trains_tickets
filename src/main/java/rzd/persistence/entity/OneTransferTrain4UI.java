package rzd.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OneTransferTrain4UI implements Comparable<OneTransferTrain4UI> {
	private String html;
	private int totalTravelTime;

	@Override
	public int compareTo(OneTransferTrain4UI o) {
		return totalTravelTime - o.totalTravelTime;
	}
}
