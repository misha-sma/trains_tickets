package rzd.scheduler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rzd.persistence.dao.CarriageDao;
import rzd.persistence.dao.SeatDao;
import rzd.persistence.dao.TrainDao;
import rzd.persistence.entity.Carriage;
import rzd.persistence.entity.Train;
import rzd.util.DateUtil;

public class CarriagesSeatsValidator {
	private static final Logger logger = LoggerFactory.getLogger(CarriagesSeatsValidator.class);

	private static final List<Carriage> trainTemplate = new ArrayList<Carriage>();

	static {
		for (int carriageNumber = 1; carriageNumber <= 20; ++carriageNumber) {
			Carriage carriage = new Carriage();
			carriage.setCarriageNumber(carriageNumber);
			int carriageType = carriageNumber <= 15 ? 1
					: (carriageNumber <= 17 ? 2 : (carriageNumber == 18 ? 3 : (carriageNumber == 19 ? 4 : 5)));
			carriage.setIdCarriageType(carriageType);
			trainTemplate.add(carriage);
		}
	}

	public static void validate() {
		long initTime = System.currentTimeMillis();
		for (int idTrain : TrainDao.TRAINS_MAP.keySet()) {
			Train train = TrainDao.TRAINS_MAP.get(idTrain);
			String depDays = train.getDepartureDays();
			Calendar cal = Calendar.getInstance();
			Calendar calTime = Calendar.getInstance();
			calTime.setTime(train.getDepartureTime());
			cal.set(Calendar.HOUR_OF_DAY, calTime.get(Calendar.HOUR_OF_DAY));
			cal.set(Calendar.MINUTE, calTime.get(Calendar.MINUTE));
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			cal.add(Calendar.DAY_OF_MONTH, -1);
			for (int delta = 0; delta <= TrainsScheduler.BUY_DAYS_COUNT; ++delta) {
				cal.add(Calendar.DAY_OF_MONTH, 1);
				if (!depDays.equals("ежд")) {
					String dayOfWeekEng = DateUtil.WEEK_DAY_FORMAT.format(cal.getTime());
					String dayOfWeek = DateUtil.DAY_OF_WEEK_MAP_ENG.get(dayOfWeekEng);
					if (!depDays.contains(dayOfWeek)) {
						continue;
					}
				}
				String dateStr = DateUtil.SIMPLE_DATE_FORMAT.format(cal.getTime());
				if (CarriageDao.isCarriagesExist(idTrain, dateStr)) {
					continue;
				}
				List<Carriage> carriages = getCarriages(idTrain, cal.getTime());
				CarriageDao.saveCarriages(carriages);
				SeatDao.addOneTrainSeats(carriages, train.getStagesCount());
			}
		}
		logger.info("Validate carriages and seats time=" + (System.currentTimeMillis() - initTime) + " ms");
	}

	private static List<Carriage> getCarriages(int idTrain, Date depDate) {
		List<Carriage> result = new ArrayList<Carriage>();
		for (Carriage carriage : trainTemplate) {
			Carriage carriageTrue = new Carriage(0, idTrain, depDate, carriage.getCarriageNumber(),
					carriage.getIdCarriageType());
			result.add(carriageTrue);
		}
		return result;
	}
}
