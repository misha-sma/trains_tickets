package rzd.server;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import rzd.persistence.dao.SeatDao;
import rzd.persistence.dao.StationDao;
import rzd.persistence.dao.TicketDao;
import rzd.persistence.dao.TrainDao;
import rzd.persistence.dao.UserDao;
import rzd.persistence.entity.Carriage;
import rzd.persistence.entity.CarriageSeatNumber;
import rzd.persistence.entity.SeatsSearchResult;
import rzd.persistence.entity.TimeTable;
import rzd.persistence.entity.Train;
import rzd.persistence.entity.TrainTravelStayTimes;
import rzd.persistence.entity.User;
import rzd.renderer.HtmlRenderer;
import rzd.renderer.SeatsRenderer;
import rzd.util.DateUtil;
import rzd.util.Util;

@Controller
public class RzdController {
	private static final Logger logger = LoggerFactory.getLogger(RzdController.class);

	public static final String HOME_PAGE;
	public static final String HOME_PAGE_BEGIN;
	public static final String HOME_PAGE_END;
	public static final byte[] FAVICON_BYTES;
	public static final String PASSENGER_PAGE;

	static {
		HOME_PAGE = Util.loadTextWithResourceAsStream("/web/index.html");
		int endBodyIndex = HOME_PAGE.indexOf("</body>");
		HOME_PAGE_BEGIN = HOME_PAGE.substring(0, endBodyIndex);
		HOME_PAGE_END = HOME_PAGE.substring(endBodyIndex);
		String passengerPage = Util.loadTextWithResourceAsStream("/web/passenger.html");
		int startIndex = passengerPage.indexOf("Данные пассажира");
		PASSENGER_PAGE = passengerPage.substring(startIndex);
		FAVICON_BYTES = Util.loadBytesWithResourceAsStream("/web/favicon.ico");
	}

	@GetMapping("/")
	public ResponseEntity getHomePage() {
		return ResponseEntity.ok(HOME_PAGE);
	}

	@GetMapping("/savePassengerData")
	public ResponseEntity savePassengerData(@RequestParam long idSeat,
			@RequestParam(name = "from") int idDepartureStation, @RequestParam(name = "to") int idDestinationStation,
			@RequestParam long passport, @RequestParam String surname, @RequestParam String name,
			@RequestParam String patronymic, @RequestParam String birthday, @RequestParam String phone,
			@RequestParam String email) {
		// сохранение данных пассажира
		User userNew = new User(passport, surname, name, patronymic, birthday, phone, email);
		User userOld = UserDao.getUserById(userNew.getIdUser());
		if (userOld == null) {
			UserDao.addUser(userNew);
		} else if (!userNew.isMatching(userOld)) {
			UserDao.updateUser(userNew);
		}
		CarriageSeatNumber carriageSN = SeatDao.getCarriageSeatNumberByIdSeat(idSeat);
		Carriage carriage = carriageSN.getCarriage();
		TicketDao.createTicket(idSeat, idDepartureStation, idDestinationStation, userNew.getIdUser());
		SeatDao.updateSeat(idSeat, idDepartureStation, idDestinationStation, carriage.getIdTrain());

		StringBuilder builder = new StringBuilder();
		builder.append("<html>\n");
		builder.append("<meta charset=\"UTF-8\" />\n");
		builder.append("<body>\n");
		builder.append("Вы успешно купили билет на ");
		int delay = TrainDao.getTravelStayTime(carriage.getIdTrain(), idDepartureStation);
		String departureDateStr = DateUtil.addMinutesToDateDate(carriage.getDepartureTime(), delay);
		String trainHeader = HtmlRenderer.getTrainHeader4Ticket(carriage.getIdTrain(), departureDateStr, delay,
				idDepartureStation, idDestinationStation);
		builder.append(trainHeader);
		builder.append("<br>\n");
		String seatInfo = HtmlRenderer.getSeatInfo(carriageSN);
		builder.append(seatInfo);
		builder.append("<br>\n");
		String userInfo = HtmlRenderer.getUserInfo(userNew);
		builder.append(userInfo);
		builder.append("</body>\n</html>");
		return ResponseEntity.ok(builder.toString());
	}

	@GetMapping("/passengerDataForm")
	public ResponseEntity passengerDataForm(@RequestParam long idSeat,
			@RequestParam(name = "from") int idDepartureStation, @RequestParam(name = "to") int idDestinationStation) {
		// введение данных пассажира
		CarriageSeatNumber carriageSN = SeatDao.getCarriageSeatNumberByIdSeat(idSeat);
		Carriage carriage = carriageSN.getCarriage();
		int delay = TrainDao.getTravelStayTime(carriage.getIdTrain(), idDepartureStation);
		String departureDateStr = DateUtil.addMinutesToDateDate(carriage.getDepartureTime(), delay);

		StringBuilder builder = new StringBuilder();
		String header = HtmlRenderer.getHeader(idDepartureStation, idDestinationStation, departureDateStr);
		builder.append(header);
		String trainHeader = HtmlRenderer.getTrainHeader(carriage.getIdTrain(), departureDateStr, delay,
				idDestinationStation);
		builder.append(trainHeader);
		builder.append("\n<br>\n");
		String seatInfo = HtmlRenderer.getSeatInfo(carriageSN);
		builder.append(seatInfo);
		builder.append("<br>\n");

		String passengerPage = PASSENGER_PAGE.replace("name=\"from\" value=\"\">",
				"name=\"from\" value=\"" + idDepartureStation + "\">");
		passengerPage = passengerPage.replace("name=\"to\" value=\"\">",
				"name=\"to\" value=\"" + idDestinationStation + "\">");
		passengerPage = passengerPage.replace("name=\"idSeat\" value=\"\">",
				"name=\"idSeat\" value=\"" + idSeat + "\">");
		builder.append(passengerPage);
		return ResponseEntity.ok(builder.toString());
	}

	@GetMapping("/seatSelection")
	public ResponseEntity seatSelection(@RequestParam(name = "from") int idDepartureStation,
			@RequestParam(name = "to") int idDestinationStation, @RequestParam int idTrain, @RequestParam String date) {
		// выбор места
		int delay = TrainDao.getTravelStayTime(idTrain, idDepartureStation);
		SeatsSearchResult ssr = SeatDao.getFreeSeats(idTrain, date, idDepartureStation, idDestinationStation, delay);
		StringBuilder builder = new StringBuilder();
		String header = HtmlRenderer.getHeader(idDepartureStation, idDestinationStation, date);
		builder.append(header);
		String trainHeader = HtmlRenderer.getTrainHeader(idTrain, date, delay, idDestinationStation);
		builder.append(trainHeader);
		for (int carriageNumber = 1; carriageNumber <= ssr.getMaxCarriageNumber(); ++carriageNumber) {
			Integer carriageType = ssr.getCarriageTypesMap().get(carriageNumber);
			if (carriageType == null) {
				continue;
			}
			String table = SeatsRenderer.renderCarriage(carriageNumber, carriageType, ssr.getSeatsMap(),
					idDepartureStation, idDestinationStation);
			builder.append(table);
		}
		builder.append(HOME_PAGE_END);
		return ResponseEntity.ok(builder.toString());
	}

	@GetMapping("/stationsSelection")
	public ResponseEntity stationsSelection(@RequestParam(name = "from") String departureStation,
			@RequestParam(name = "to") String destinationStation, @RequestParam String date) {
		// выбор станций
		departureStation = departureStation.toLowerCase();
		destinationStation = destinationStation.toLowerCase();

		int idDepartureStation = StationDao.STATIONS_NAME_ID_MAP.get(departureStation);
		int idDestinationStation = StationDao.STATIONS_NAME_ID_MAP.get(destinationStation);
		StringBuilder builder = new StringBuilder();
		String header = HtmlRenderer.getHeader(idDepartureStation, idDestinationStation, date);
		builder.append(header);
		builder.append("<table border=\"1\">\n");
		builder.append(
				"<tr>\n<th>Поезд</th>\n<th>Время отправления</th>\n<th>Время в пути</th>\n<th>Время прибытия</th>\n");
		boolean isAllDays = date == null || date.isEmpty();
		if (isAllDays) {
			builder.append("<th>Выбрать дату</th>\n</tr>\n");
		} else {
			builder.append("<th></th>\n</tr>\n");
		}
		List<TrainTravelStayTimes> trains = TrainDao.getTrainsByStations(idDepartureStation, idDestinationStation);
		if (!isAllDays) {
			trains = filterTrains4OneDay(trains, date);
		}
		for (TrainTravelStayTimes trainTravelStayTimes : trains) {
			int idTrain = trainTravelStayTimes.getIdTrain();
			Train train = TrainDao.TRAINS_MAP.get(idTrain);
			String depTime = trainTravelStayTimes.getDepartureTime();
			String destTime = DateUtil.addMinutesToDate(train.getDepartureTime(),
					trainTravelStayTimes.getDestinationTravelTime());
			builder.append("<tr><td>")
					.append("<a href=\"/timetable?idTrain=" + idTrain + "&from=" + idDepartureStation + "&to="
							+ idDestinationStation + "\">" + idTrain + " " + train.getDepartureStation() + " - "
							+ train.getDestinationStation() + "</a>");
			if (train.getName() != null) {
				builder.append("<br>" + "&laquo;" + train.getName() + "&raquo;");
			}
			builder.append("</td>\n<td>" + depTime + "</td>\n<td>"
					+ DateUtil.formatMinutes(trainTravelStayTimes.getDestinationTravelTime()
							- trainTravelStayTimes.getDepartureTravelStayTime())
					+ "</td>\n<td>" + destTime + "</td>\n");
			if (isAllDays) {
				builder.append(
						"<td>\n<form autocomplete=\"off\" method='get' name='selectDate' action=\"/seatSelection\">\n"
								+ "<input type=\"text\" name=\"date\" class=\"tcal\" value=\"\" style=\"width:200px;\">\n"
								+ "<input type=\"hidden\" name=\"idTrain\" value=\"" + idTrain + "\">\n"
								+ "<input type=\"hidden\" name=\"from\" value=\"" + idDepartureStation + "\">\n"
								+ "<input type=\"hidden\" name=\"to\" value=\"" + idDestinationStation + "\">\n"
								+ "<input type=\"submit\" name=\"submitDateButton\" value=\"Выбрать\">\n");
			} else {
				builder.append("<td>\n<form method='get' name='selectSeat' action=\"/seatSelection\">\n"
						+ "<input type=\"hidden\" name=\"date\" value=\"" + date + "\">\n"
						+ "<input type=\"hidden\" name=\"idTrain\" value=\"" + idTrain + "\">\n"
						+ "<input type=\"hidden\" name=\"from\" value=\"" + idDepartureStation + "\">\n"
						+ "<input type=\"hidden\" name=\"to\" value=\"" + idDestinationStation + "\">\n"
						+ "<input type=\"submit\" name=\"selectSeatButton\" value=\"Выбрать место\">\n");
			}
			String depDays = isAllDays ? getDepartureDaysTrue(train, trainTravelStayTimes.getDepartureTravelStayTime())
					: "";
			builder.append("</form>\n" + depDays + "</td>\n</tr>\n");
		}
		builder.append("</table>\n");
		builder.append(HOME_PAGE_END);
		return ResponseEntity.ok(builder.toString());
	}

	@GetMapping("/timetable")
	public ResponseEntity timetable(@RequestParam(name = "from") int idDepartureStation,
			@RequestParam(name = "to") int idDestinationStation, @RequestParam int idTrain) {
		// страница с расписанием поезда
		List<TimeTable> timeTable = TrainDao.getTimeTable(idTrain);
		Train train = TrainDao.TRAINS_MAP.get(idTrain);
		StringBuilder builder = new StringBuilder();
		builder.append("<html>\n<meta charset=\"UTF-8\" />\n<body>\n");
		builder.append("Расписание поезда №" + idTrain + " " + train.getDepartureStation() + " - "
				+ train.getDestinationStation() + "<br><br>\n");
		builder.append("<table>\n");
		builder.append(
				"<tr>\n<th>Станция</th>\n<th>Прибытие</th>\n<th>Стоянка</th>\n<th>Отправление</th>\n<th>Время в пути</th>\n</tr>\n");
		for (TimeTable tt : timeTable) {
			String color = tt.getIdStation() == idDepartureStation || tt.getIdStation() == idDestinationStation
					? " style=\"background-color:lime;\""
					: "";
			// название станции
			builder.append("<tr" + color + "><td>" + StationDao.STATIONS_ID_NAME_MAP.get(tt.getIdStation()) + "</td>");
			// время прибытия
			if (tt.getTravelTime() == 0) {
				builder.append("<td></td>");
			} else {
				builder.append(
						"<td>" + DateUtil.addMinutesToDate(train.getDepartureTime(), tt.getTravelTime()) + "</td>");
			}
			// стоянка
			if (tt.getTravelTime() == 0 || tt.getStayTime() == -1) {
				builder.append("<td></td>");
			} else {
				builder.append("<td>" + DateUtil.formatMinutes(tt.getStayTime()) + "</td>");
			}
			// время отправления
			if (tt.getStayTime() == -1) {
				builder.append("<td></td>");
			} else {
				builder.append("<td>"
						+ DateUtil.addMinutesToDate(train.getDepartureTime(), tt.getTravelTime() + tt.getStayTime())
						+ "</td>");
			}
			// время в пути
			if (tt.getTravelTime() == 0) {
				builder.append("<td></td>");
			} else {
				builder.append("<td>" + DateUtil.formatMinutes(tt.getTravelTime()) + "</td>");
			}
			builder.append("</tr>\n");
		}
		builder.append("</table>\n</body>\n</html>");
		return ResponseEntity.ok(builder.toString());
	}

	@GetMapping("/suggesting")
	public ResponseEntity suggesting(@RequestParam String suggest) {
		// саджестинг выбора станций
		String letters = suggest.toLowerCase();
		List<String> suggestions = StationDao.SUGGESTING_MAP.get(letters);
		if (suggestions == null || suggestions.isEmpty()) {
			String lettersRus = StationDao.translit(letters);
			suggestions = StationDao.SUGGESTING_MAP.get(lettersRus);
			if (suggestions == null || suggestions.isEmpty()) {
				BodyBuilder b = ResponseEntity.ok();
				b.contentType(MediaType.APPLICATION_JSON);
				return b.body("{\"suggestions\": []}");
			}
		}
		StringBuilder builder = new StringBuilder();
		builder.append("{\"suggestions\": [");
		int i = 0;
		for (String name : suggestions) {
			if (i > 0) {
				builder.append(", ");
			}
			builder.append("\"" + name + "\"");
			++i;
		}
		builder.append("]}");
		BodyBuilder b = ResponseEntity.ok();
		b.contentType(MediaType.APPLICATION_JSON);
		return b.body(builder.toString());
	}

	@GetMapping("/**")
	public ResponseEntity redirect() {
		BodyBuilder b = ResponseEntity.status(301);
		b.header("Location", "/");
		return b.build();
	}

	private static List<TrainTravelStayTimes> filterTrains4OneDay(List<TrainTravelStayTimes> trains, String date) {
		List<TrainTravelStayTimes> result = new LinkedList<TrainTravelStayTimes>();
		for (TrainTravelStayTimes trainTravelStayTimes : trains) {
			int idTrain = trainTravelStayTimes.getIdTrain();
			Train train = TrainDao.TRAINS_MAP.get(idTrain);
			String depDays = train.getDepartureDays();
			if (depDays.equals("ежд")) {
				result.add(trainTravelStayTimes);
				continue;
			}
			Date d = DateUtil.string2DateTime(date + " " + trainTravelStayTimes.getDepartureTime());
			Calendar calendarDep = Calendar.getInstance();
			calendarDep.setTime(d);
			calendarDep.add(Calendar.MINUTE, -trainTravelStayTimes.getDepartureTravelStayTime());
			int depDayTrainWeekInt = calendarDep.get(Calendar.DAY_OF_WEEK);
			String depDayTrainWeek = DateUtil.DAY_OF_WEEK_MAP.get(depDayTrainWeekInt);
			if (depDays.contains(depDayTrainWeek)) {
				result.add(trainTravelStayTimes);
			}
		}
		return result;
	}

	private static String getDepartureDaysTrue(Train train, int departureTravelStayTime) {
		String depDays = train.getDepartureDays();
		if (depDays.equals("ежд")) {
			return depDays;
		}
		Calendar calendarDep = Calendar.getInstance();
		calendarDep.setTime(train.getDepartureTime());
		calendarDep.add(Calendar.MINUTE, departureTravelStayTime);
		int deltaDays = calendarDep.get(Calendar.DAY_OF_MONTH) - 1;
		if (deltaDays == 0) {
			return depDays;
		}
		StringBuilder builder = new StringBuilder();
		String[] parts = depDays.split(",");
		for (int i = 0; i < parts.length; ++i) {
			String part = parts[i];
			int dayOfWeekInt = DateUtil.DAY_OF_WEEK_MAP_REVERSE.get(part);
			dayOfWeekInt += deltaDays;
			dayOfWeekInt = dayOfWeekInt % 7;
			dayOfWeekInt = dayOfWeekInt == 0 ? 7 : dayOfWeekInt;
			String dayOfWeekTrue = DateUtil.DAY_OF_WEEK_MAP.get(dayOfWeekInt);
			if (i > 0) {
				builder.append(",");
			}
			builder.append(dayOfWeekTrue);
		}
		return builder.toString();
	}
}
