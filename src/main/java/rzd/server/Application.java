package rzd.server;

import java.util.Locale;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import rzd.persistence.dao.CarriageDao;
import rzd.persistence.dao.StationDao;
import rzd.persistence.dao.TrainDao;
import rzd.scheduler.TrainsScheduler;

/**
 * Hello world!
 *
 */
@SpringBootApplication
public class Application {
	private static final Logger logger = LoggerFactory.getLogger(Application.class);

	public static void main(String[] args) {
		Locale.setDefault(Locale.ENGLISH);
		loadCaches();
		// CarriagesSeatsValidator.validate();
		TrainsScheduler.start();

		SpringApplication app = new SpringApplication(Application.class);
		Properties properties = new Properties();
		properties.setProperty("spring.resources.static-locations", "classpath:/web/");
		app.setDefaultProperties(properties);
		app.run(args);
	}

	private static void loadCaches() {
		long initTime = System.currentTimeMillis();
		CarriageDao.loadCarriageCaches();
		StationDao.loadStationsCaches();
		TrainDao.loadTrainsCache();
		logger.info("Load caches time=" + (System.currentTimeMillis() - initTime) + " ms");
	}
}
