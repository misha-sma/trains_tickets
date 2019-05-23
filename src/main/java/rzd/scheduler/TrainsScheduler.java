package rzd.scheduler;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rzd.persistence.dao.CarriageDao;
import rzd.persistence.dao.SeatDao;
import rzd.persistence.dao.TrainDao;
import rzd.persistence.entity.Carriage;
import rzd.persistence.entity.Train;
import rzd.util.DateUtil;

public class TrainsScheduler {
	private static final Logger logger = LoggerFactory.getLogger(TrainsScheduler.class);

	public static final int BUY_DAYS_COUNT = 45;
	public static final long PERIOD = 24 * 3600 * 1000;
	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	public static void start() {
		long initialDelay = PERIOD - (System.currentTimeMillis() + 3 * 3600 * 1000) % PERIOD + 10000;
		logger.info("Trains scheduler initial delay=" + initialDelay + " ms");
		scheduler.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				long initTime = System.currentTimeMillis();
				logger.info("Trains scheduler started!!!");
				for (int idTrain : TrainDao.TRAINS_MAP.keySet()) {
					Train train = TrainDao.TRAINS_MAP.get(idTrain);
					String depDays = train.getDepartureDays();
					if (depDays.equals("ежд")) {
						Calendar cal = Calendar.getInstance();
						cal.add(Calendar.DAY_OF_MONTH, BUY_DAYS_COUNT - 1);
						String dateLast = DateUtil.SIMPLE_DATE_FORMAT.format(cal.getTime());
						addCarriagesAndSeats(train, idTrain, dateLast);
						continue;
					}
					Calendar cal = Calendar.getInstance();
					cal.add(Calendar.DAY_OF_MONTH, BUY_DAYS_COUNT);
					String dayOfWeekEng = DateUtil.WEEK_DAY_FORMAT.format(cal.getTime());
					String dayOfWeek = DateUtil.DAY_OF_WEEK_MAP_ENG.get(dayOfWeekEng);
					if (!depDays.contains(dayOfWeek)) {
						continue;
					}
					String dateLast = null;
					for (int delta = 1; delta <= 7; ++delta) {
						cal.add(Calendar.DAY_OF_MONTH, -1);
						dayOfWeekEng = DateUtil.WEEK_DAY_FORMAT.format(cal.getTime());
						dayOfWeek = DateUtil.DAY_OF_WEEK_MAP_ENG.get(dayOfWeekEng);
						if (depDays.contains(dayOfWeek)) {
							dateLast = DateUtil.SIMPLE_DATE_FORMAT.format(cal.getTime());
							break;
						}
					}
					if (dateLast == null) {
						logger.error("Last departure day not found!!! idTrain=" + idTrain);
					}
					addCarriagesAndSeats(train, idTrain, dateLast);
				}
				logger.info("Trains scheduler time=" + (System.currentTimeMillis() - initTime) + " ms");
			}
		}, initialDelay, PERIOD, TimeUnit.MILLISECONDS);
	}

	private static void addCarriagesAndSeats(Train train, int idTrain, String dateLast) {
		Date depTimeTrue = getDepTime(train.getDepartureTime());
		List<Carriage> carriages = CarriageDao.getCarriages(idTrain, dateLast);
		for (Carriage carriage : carriages) {
			carriage.setDepartureTime(depTimeTrue);
		}
		CarriageDao.saveCarriages(carriages);
		SeatDao.addOneTrainSeats(carriages, train.getStagesCount());
	}

	private static Date getDepTime(Date depTime) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_MONTH, BUY_DAYS_COUNT);
		Calendar calTime = Calendar.getInstance();
		calTime.setTime(depTime);
		cal.set(Calendar.HOUR_OF_DAY, calTime.get(Calendar.HOUR_OF_DAY));
		cal.set(Calendar.MINUTE, calTime.get(Calendar.MINUTE));
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

	public static void main(String[] args) {
		System.out.println(PERIOD - (System.currentTimeMillis() + 3 * 3600 * 1000) % PERIOD);
	}
}
