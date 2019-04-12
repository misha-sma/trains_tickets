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
				for (int idTrain : TrainDao.TRAINS_MAP.keySet()) {
					Train train = TrainDao.TRAINS_MAP.get(idTrain);
					String depDays = train.getDepartureDays();
					if (depDays.equals("ежд")) {
						Calendar cal = Calendar.getInstance();
						cal.add(Calendar.DAY_OF_MONTH, BUY_DAYS_COUNT - 1);
						String dateLast = DateUtil.SIMPLE_DATE_FORMAT.format(cal.getTime());
						List<Carriage> carriages = CarriageDao.getCarriages(idTrain, dateLast);
						for (Carriage carriage : carriages) {
							carriage.setDepartureTime(new Date(carriage.getDepartureTime().getTime() + PERIOD));
						}
						CarriageDao.saveCarriages(carriages);
						for (Carriage carriage : carriages) {
							SeatDao.addOneCarriageSeats(carriage);
						}
						continue;
					}

				}
			}
		}, initialDelay, PERIOD, TimeUnit.MILLISECONDS);
	}

	public static void main(String[] args) {
		System.out.println(PERIOD - (System.currentTimeMillis() + 3 * 3600 * 1000) % PERIOD);
	}
}
