package rzd.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rzd.persistence.dao.CarriageDao;
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
import rzd.renderer.OneTransferRenderer;
import rzd.renderer.SeatsRenderer;
import rzd.scheduler.CarriagesSeatsValidator;
import rzd.scheduler.TrainsScheduler;
import rzd.util.DateUtil;
import rzd.util.Util;

public class HttpServer {
	private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);

	public static final String HOME_PAGE;
	public static final String HOME_PAGE_BEGIN;
	public static final String HOME_PAGE_END;
	public static final byte[] FAVICON_BYTES;
	public static final String PASSENGER_PAGE;

	public static final int PORT = 8080;

	public static final String URL_REGEXP = "GET /?([^ ]*) ";
	public static final Pattern URL_PATTERN = Pattern.compile(URL_REGEXP);

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

	private static class SocketProcessor implements Runnable {
		private Socket socket;
		private InputStream is;
		private OutputStream os;

		private SocketProcessor(Socket socket) throws IOException {
			this.socket = socket;
			this.is = socket.getInputStream();
			this.os = socket.getOutputStream();
		}

		public void run() {
			try (Socket socketLocal = socket) {
				String url = getUrl();
				url = URLDecoder.decode(url, "UTF8");
				logger.info("url=" + url);
				if (url.equals("favicon.ico")) {
					writeFaviconResponse();
				} else if (url.isEmpty() || url.equals("/")) {
					writeHtmlResponse(HOME_PAGE);
				} else if (url.startsWith("?surname=")) {
					// сохранение данных пассажира
					Map<String, String> params = Util.parseParameters(url);
					User userNew = new User(params);
					User userOld = UserDao.getUserById(userNew.getIdUser());
					if (userOld == null) {
						UserDao.addUser(userNew);
					} else if (!userNew.isMatching(userOld)) {
						UserDao.updateUser(userNew);
					}
					long idSeat = Long.parseLong(params.get("idSeat"));
					int idDepartureStation = Integer.parseInt(params.get("from"));
					int idDestinationStation = Integer.parseInt(params.get("to"));
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
					String trainHeader = HtmlRenderer.getTrainHeader4Ticket(carriage.getIdTrain(), departureDateStr,
							delay, idDepartureStation, idDestinationStation);
					builder.append(trainHeader);
					builder.append("<br>\n");
					String seatInfo = HtmlRenderer.getSeatInfo(carriageSN);
					builder.append(seatInfo);
					builder.append("<br>\n");
					String userInfo = HtmlRenderer.getUserInfo(userNew);
					builder.append(userInfo);
					builder.append("</body>\n</html>");
					writeHtmlResponse(builder.toString());
				} else if (url.startsWith("?idSeat=")) {
					// введение данных пассажира
					Map<String, String> params = Util.parseParameters(url);
					long idSeat = Long.parseLong(params.get("idSeat"));
					int idDepartureStation = Integer.parseInt(params.get("from"));
					int idDestinationStation = Integer.parseInt(params.get("to"));
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
					writeHtmlResponse(builder.toString());
				} else if (url.startsWith("?date=")) {
					// выбор места
					Map<String, String> params = Util.parseParameters(url);
					int idDepartureStation = Integer.parseInt(params.get("from"));
					int idDestinationStation = Integer.parseInt(params.get("to"));
					int idTrain = Integer.parseInt(params.get("idTrain"));
					String date = params.get("date");
					int delay = TrainDao.getTravelStayTime(idTrain, idDepartureStation);
					SeatsSearchResult ssr = SeatDao.getFreeSeats(idTrain, date, idDepartureStation,
							idDestinationStation, delay);
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
					writeHtmlResponse(builder.toString());
				} else if (url.startsWith("?from=")) {
					// выбор станций
					Map<String, String> params = Util.parseParameters(url);
					String departureStation = params.get("from").toLowerCase();
					String destinationStation = params.get("to").toLowerCase();
					int idDepartureStation = StationDao.STATIONS_NAME_ID_MAP.get(departureStation);
					int idDestinationStation = StationDao.STATIONS_NAME_ID_MAP.get(destinationStation);
					String date = params.get("date");
					StringBuilder builder = new StringBuilder();
					String header = HtmlRenderer.getHeader(idDepartureStation, idDestinationStation, date);
					builder.append(header);
					builder.append("<table border=\"1\">\n");
					List<TrainTravelStayTimes> trains = TrainDao.getTrainsByStations(idDepartureStation,
							idDestinationStation);
					if (trains.isEmpty()) {
						builder.append(
								"<tr>\n<th>Первый поезд</th>\n<th>Время отправления</th>\n<th>Время в пути до станции пересадки</th>\n<th>Время прибытия на станцию пересадки</th>\n"
										+ "<th>Станция пересадки</th>\n<th>Время ожидания на станции пересадки</th>\n<th>Второй поезд</th>\n"
										+ "<th>Время отправления от станции пересадки</th>\n<th>Время в пути до конечной станции</th>\n"
										+ "<th>Время прибытия на конечную станцию</th>\n<th>Общее время в пути</th>\n<th>Дни отправления</th>\n");
						String html = OneTransferRenderer.oneTransferRoutesSearch(idDepartureStation,
								idDestinationStation);
						builder.append(html);
						builder.append("</table>\n");
						builder.append(HOME_PAGE_END);
						writeHtmlResponse(builder.toString());
						return;
					}
					builder.append(
							"<tr>\n<th>Поезд</th>\n<th>Время отправления</th>\n<th>Время в пути</th>\n<th>Время прибытия</th>\n");
					boolean isAllDays = date == null || date.isEmpty();
					if (isAllDays) {
						builder.append("<th>Выбрать дату</th>\n</tr>\n");
					} else {
						builder.append("<th></th>\n</tr>\n");
					}
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
								.append("<a href=\"?idTrain=" + idTrain + "&from=" + idDepartureStation + "&to="
										+ idDestinationStation + "\">" + idTrain + " " + train.getDepartureStation()
										+ " - " + train.getDestinationStation() + "</a>");
						if (train.getName() != null) {
							builder.append("<br>" + "&laquo;" + train.getName() + "&raquo;");
						}
						builder.append("</td>\n<td>" + depTime + "</td>\n<td>"
								+ DateUtil.formatMinutes(trainTravelStayTimes.getDestinationTravelTime()
										- trainTravelStayTimes.getDepartureTravelStayTime())
								+ "</td>\n<td>" + destTime + "</td>\n");
						if (isAllDays) {
							builder.append("<td>\n<form autocomplete=\"off\" method='get' name='selectDate'>\n"
									+ "<input type=\"text\" name=\"date\" class=\"tcal\" value=\"\" style=\"width:200px;\">\n"
									+ "<input type=\"hidden\" name=\"idTrain\" value=\"" + idTrain + "\">\n"
									+ "<input type=\"hidden\" name=\"from\" value=\"" + idDepartureStation + "\">\n"
									+ "<input type=\"hidden\" name=\"to\" value=\"" + idDestinationStation + "\">\n"
									+ "<input type=\"submit\" name=\"submitDateButton\" value=\"Выбрать\">\n");
						} else {
							builder.append("<td>\n<form method='get' name='selectSeat'>\n"
									+ "<input type=\"hidden\" name=\"date\" value=\"" + date + "\">\n"
									+ "<input type=\"hidden\" name=\"idTrain\" value=\"" + idTrain + "\">\n"
									+ "<input type=\"hidden\" name=\"from\" value=\"" + idDepartureStation + "\">\n"
									+ "<input type=\"hidden\" name=\"to\" value=\"" + idDestinationStation + "\">\n"
									+ "<input type=\"submit\" name=\"selectSeatButton\" value=\"Выбрать место\">\n");
						}
						String depDays = isAllDays
								? getDepartureDaysTrue(train, trainTravelStayTimes.getDepartureTravelStayTime())
								: "";
						builder.append("</form>\n" + depDays + "</td>\n</tr>\n");
					}
					builder.append("</table>\n");
					builder.append(HOME_PAGE_END);
					writeHtmlResponse(builder.toString());
				} else if (url.startsWith("?idTrain=")) {
					// страница с расписанием поезда
					Map<String, String> params = Util.parseParameters(url);
					int idTrain = Integer.parseInt(params.get("idTrain"));
					int idDepartureStation = Integer.parseInt(params.get("from"));
					int idDestinationStation = Integer.parseInt(params.get("to"));
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
						String color = tt.getIdStation() == idDepartureStation
								|| tt.getIdStation() == idDestinationStation ? " style=\"background-color:lime;\"" : "";
						// название станции
						builder.append("<tr" + color + "><td>" + StationDao.STATIONS_ID_NAME_MAP.get(tt.getIdStation())
								+ "</td>");
						// время прибытия
						if (tt.getTravelTime() == 0) {
							builder.append("<td></td>");
						} else {
							builder.append(
									"<td>" + DateUtil.addMinutesToDate(train.getDepartureTime(), tt.getTravelTime())
											+ "</td>");
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
							builder.append("<td>" + DateUtil.addMinutesToDate(train.getDepartureTime(),
									tt.getTravelTime() + tt.getStayTime()) + "</td>");
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
					writeHtmlResponse(builder.toString());
				} else if (url.startsWith("?suggest=")) {
					// саджестинг выбора станций
					Map<String, String> params = Util.parseParameters(url);
					String letters = params.get("suggest").toLowerCase();
					List<String> suggestions = StationDao.SUGGESTING_MAP.get(letters);
					if (suggestions == null || suggestions.isEmpty()) {
						String lettersRus = StationDao.translit(letters);
						suggestions = StationDao.SUGGESTING_MAP.get(lettersRus);
						if (suggestions == null || suggestions.isEmpty()) {
							writeJsonResponse("{\"suggestions\": []}");
							return;
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
					writeJsonResponse(builder.toString());
				} else if (url.startsWith("script/") || url.startsWith("css/")) {
					String text = Util.loadTextWithResourceAsStream("/web/" + url);
					writeHtmlResponse(text);
				} else if (url.startsWith("images/")) {
					byte[] imageBytes = Util.loadBytesWithResourceAsStream("/web/" + url);
					int dotPos = url.lastIndexOf('.') + 1;
					String extention = url.substring(dotPos).toLowerCase();
					writeImageResponse(imageBytes, extention);
				} else {
					writeRedirectResponse();
				}
			} catch (Throwable t) {
				logger.error(t.getMessage(), t);
			}
		}

		private void writeJsonResponse(String json) throws IOException {
			String response = "HTTP/1.1 200 OK\r\n" + "Server: misha-sma-Server/2012\r\n"
					+ "Content-Type: application/json\r\n" + "Connection: close\r\n\r\n";
			String result = response + json;
			os.write(result.getBytes());
			os.flush();
		}

		private void writeHtmlResponse(String html) throws IOException {
			String response = "HTTP/1.1 200 OK\r\n" + "Server: misha-sma-Server/2012\r\n"
					+ "Content-Type: text/html\r\n" + "Connection: close\r\n\r\n";
			String result = response + html;
			os.write(result.getBytes());
			os.flush();
		}

		private void writeRedirectResponse() throws IOException {
			String response = "HTTP/1.1 301 Moved Permanently\r\n" + "Server: misha-sma-Server/2012\r\n"
					+ "Location: /\r\n" + "Connection: close\r\n\r\n";
			os.write(response.getBytes());
			os.flush();
		}

		private void writeFaviconResponse() throws IOException {
			String response = "HTTP/1.1 200 OK\r\n" + "Server: misha-sma-Server/2012\r\n"
					+ "Content-Type: image/vnd.microsoft.icon\r\n" + "Content-Length: " + FAVICON_BYTES.length + "\r\n"
					+ "Connection: close\r\n\r\n";
			os.write(response.getBytes());
			os.write(FAVICON_BYTES);
			os.flush();
		}

		private void writeImageResponse(byte[] imageBytes, String extention) throws IOException {
			String response = "HTTP/1.1 200 OK\r\n" + "Server: misha-sma-Server/2012\r\n" + "Content-Type: image/"
					+ extention + "\r\n" + "Content-Length: " + imageBytes.length + "\r\n"
					+ "Connection: close\r\n\r\n";
			os.write(response.getBytes());
			os.write(imageBytes);
			os.flush();
		}

		private String getUrl() throws IOException {
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line;
			while ((line = br.readLine()) != null && !line.trim().isEmpty()) {
				Matcher m = URL_PATTERN.matcher(line);
				if (m.find()) {
					return m.group(1);
				}
			}
			return "";
		}
	}

	public static void main(String[] args) throws Throwable {
		Locale.setDefault(Locale.ENGLISH);
		loadCaches();
		CarriagesSeatsValidator.validate();
		TrainsScheduler.start();
		ServerSocket serverSocket = new ServerSocket(PORT);
		logger.info("Server started on port " + PORT + " !!!");
		while (true) {
			Socket socket = serverSocket.accept();
			new Thread(new SocketProcessor(socket)).start();
		}
	}

	private static void loadCaches() {
		long initTime = System.currentTimeMillis();
		CarriageDao.loadCarriageCaches();
		StationDao.loadStationsCaches();
		TrainDao.loadTrainsCache();
		logger.info("Load caches time=" + (System.currentTimeMillis() - initTime) + " ms");
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
