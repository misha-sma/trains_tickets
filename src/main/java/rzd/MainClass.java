package rzd;

import java.util.Date;
import java.util.List;
import java.util.Map;

import rzd.persistence.dao.CarriageDao;
import rzd.persistence.dao.SeatDao;
import rzd.persistence.dao.TrainDao;
import rzd.persistence.entity.Carriage;

public class MainClass {
	public static Map<Integer, Integer> SEATS_COUNT_MAP = CarriageDao.getSeatsCountMap();

	public static void main(String[] args) {
		Date ddd = new Date(System.currentTimeMillis());
		List<Carriage> carriages = CarriageDao.getCarriages();
		for (Carriage carriage : carriages) {
			// SeatDao.addOneCarriageSeats(carriage);
		}
		List<Integer> idTrains = TrainDao.getTrainsByStations(1, 7);
		System.out.println("idTrains=" + idTrains);
//		List<Integer> idTrainsDate = TrainDao.getTrainsByStationsAndDate(1, 7, new Date(118, 11, 14));
        List<Integer> idTrainsDate = TrainDao.getTrainsByStationsAndDate(1, 7, "2018-12-14");
		System.out.println("idTrainsDate=" + idTrainsDate);
		System.out.println("ENDDDD!!!!!!");
	}
}
