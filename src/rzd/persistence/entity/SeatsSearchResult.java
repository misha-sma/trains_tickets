package rzd.persistence.entity;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SeatsSearchResult {
	private int maxCarriageNumber;
	private Map<Integer, Integer> carriageTypesMap;
	private Map<Integer, Long> seatsMap;
}
