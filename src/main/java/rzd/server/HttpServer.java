package rzd.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.List;
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
import rzd.persistence.entity.SeatsSearchResult;
import rzd.persistence.entity.Train;
import rzd.persistence.entity.TrainTravelStayTimes;
import rzd.persistence.entity.User;
import rzd.renderer.HtmlRenderer;
import rzd.renderer.SeatsRenderer;
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
//				logger.info("url=" + url);
				url = URLDecoder.decode(url, "UTF8");
				logger.info("url=" + url);
				if (url.equals("favicon.ico")) {
					writeFaviconResponse();
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
					TicketDao.createTicket(idSeat, idDepartureStation, idDestinationStation, userNew.getIdUser());
					SeatDao.updateSeat(idSeat, idDepartureStation, idDestinationStation);
					StringBuilder builder = new StringBuilder();
					builder.append("<html>\n");
					builder.append("<meta charset=\"UTF-8\" />\n");
					builder.append("<body>\n");
					builder.append("Вы успешно купили билет на ");
					int idTrain = SeatDao.getIdTrainByIdSeat(idSeat);
					int delay = TrainDao.getTravelStayTime(idTrain, idDepartureStation);
					String date = SeatDao.getDepartureDateByIdSeat(idSeat, idDepartureStation);
					String trainHeader = HtmlRenderer.getTrainHeader4Ticket(idTrain, date, delay, idDepartureStation,
							idDestinationStation);
					builder.append(trainHeader);
					builder.append("<br>\n");
					String seatInfo = HtmlRenderer.getSeatInfo(idSeat);
					builder.append(seatInfo);
					builder.append("<br>\n");
					String userInfo = HtmlRenderer.getUserInfo(userNew);
					builder.append(userInfo);
					builder.append("</body>\n</html>");
					writeHomePageResponse(builder.toString());
				} else if (url.startsWith("?idSeat=")) {
					// введение данных пассажира
					Map<String, String> params = Util.parseParameters(url);
					long idSeat = Long.parseLong(params.get("idSeat"));
					int idDepartureStation = Integer.parseInt(params.get("from"));
					int idDestinationStation = Integer.parseInt(params.get("to"));
					String date = SeatDao.getDepartureDateByIdSeat(idSeat, idDepartureStation);
					StringBuilder builder = new StringBuilder();
					String header = HtmlRenderer.getHeader(idDepartureStation, idDestinationStation, date);
					builder.append(header);
					int idTrain = SeatDao.getIdTrainByIdSeat(idSeat);
					int delay = TrainDao.getTravelStayTime(idTrain, idDepartureStation);
					String trainHeader = HtmlRenderer.getTrainHeader(idTrain, date, delay, idDestinationStation);
					builder.append(trainHeader);
					builder.append("\n<br>\n");
					String seatInfo = HtmlRenderer.getSeatInfo(idSeat);
					builder.append(seatInfo);
					builder.append("<br>\n");

					String passengerPage = PASSENGER_PAGE.replace("name=\"from\" value=\"\">",
							"name=\"from\" value=\"" + idDepartureStation + "\">");
					passengerPage = passengerPage.replace("name=\"to\" value=\"\">",
							"name=\"to\" value=\"" + idDestinationStation + "\">");
					passengerPage = passengerPage.replace("name=\"idSeat\" value=\"\">",
							"name=\"idSeat\" value=\"" + idSeat + "\">");
					builder.append(passengerPage);
					writeHomePageResponse(builder.toString());
				} else if (url.startsWith("?date=")) {
					// выбор места
					Map<String, String> params = Util.parseParameters(url);
					// String departureStation = params.get("from");
					// String destinationStation = params.get("to");
					int idDepartureStation = Integer.parseInt(params.get("from"));
					int idDestinationStation = Integer.parseInt(params.get("to"));
					int idTrain = Integer.parseInt(params.get("idTrain"));
					String date = params.get("date");
					int delay = TrainDao.getTravelStayTime(idTrain, idDepartureStation);
					SeatsSearchResult ssr = SeatDao.getFreeSeats(idTrain, date, idDepartureStation,
							idDestinationStation, delay);
					StringBuilder builder = new StringBuilder();
//                    String departureStation = StationDao.getStationNameById(idDepartureStation);
//                    String destinationStation = StationDao.getStationNameById(idDestinationStation);
//                    String header = HOME_PAGE_BEGIN.replace("placeholder=\"Откуда\"", "value=\"" + departureStation + "\"");
//                    header = header.replace("placeholder=\"Куда\"", "value=\"" + destinationStation + "\"");
//                    header = header.replace("value=\"\"", "value=\"" + date + "\"");
					String header = HtmlRenderer.getHeader(idDepartureStation, idDestinationStation, date);
					builder.append(header);
//                    String departureStationTrain = TrainDao.getDepartureStation(idTrain);
//                    String destinationStationTrain = TrainDao.getDestinationStation(idTrain);
//                    Train train=TrainDao.getTrainById(idTrain);
//                    String trainNameQuotes=train.getName()==null?"":"&laquo;"+train.getName()+"&raquo;";
//                    builder.append("Поезд №"+idTrain+" "+departureStationTrain+ " - "+destinationStationTrain+" "+trainNameQuotes+"<br>");
//                    String depTime=Util.addMinutesToDate(train.getDepartureTime(), delay);
//                    builder.append("Отправление "+Util.convertDateToDots(date)+" "+Util.getDayOfWeek(date)+" в "+depTime+"<br>");
//                    int delayDest = TrainDao.getTravelTime(idTrain, idDestinationStation);
//                    String[] destDate=Util.calcDestDate(date, depTime, delayDest-delay);
//                    builder.append("Прибытие " + destDate[0] +" "+destDate[2]+ " в " + destDate[1] + "<br>");
//                    builder.append("Время в пути "+Util.formatMinutes(delayDest-delay)+"<br>");
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
					writeHomePageResponse(builder.toString());
				} else if (url.startsWith("?from=")) {
					// выбор станций
					Map<String, String> params = Util.parseParameters(url);
					String departureStation = params.get("from").toLowerCase();
					String destinationStation = params.get("to").toLowerCase();
					int idDepartureStation = StationDao.STATIONS_NAME_ID_MAP.get(departureStation);
					int idDestinationStation = StationDao.STATIONS_NAME_ID_MAP.get(destinationStation);
					String departureStationTrue = StationDao.STATIONS_ID_NAME_MAP.get(idDepartureStation);
					String destinationStationTrue = StationDao.STATIONS_ID_NAME_MAP.get(idDestinationStation);
					String date = params.get("date");
					StringBuilder builder = new StringBuilder();
					String header = HOME_PAGE_BEGIN.replace("placeholder=\"Откуда\"",
							"value=\"" + departureStationTrue + "\"");
					header = header.replace("placeholder=\"Куда\"", "value=\"" + destinationStationTrue + "\"");
					header = header.replace("value=\"\"", "value=\"" + date + "\"");
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
					List<TrainTravelStayTimes> trains = null;
					if (isAllDays) {
						trains = TrainDao.getTrainsByStations(idDepartureStation, idDestinationStation);
					} else {
						trains = TrainDao.getTrainsByStationsAndDate(idDepartureStation, idDestinationStation, date);
					}

					for (TrainTravelStayTimes trainTravelStayTimes : trains) {
						int idTrain=trainTravelStayTimes.getIdTrain();
						Train train=TrainDao.TRAINS_MAP.get(idTrain);
						String depTime = Util.addMinutesToDate(train.getDepartureTime(), trainTravelStayTimes.getDepartureTravelStayTime());
						String destTime = Util.addMinutesToDate(train.getDepartureTime(), trainTravelStayTimes.getDestinationTravelTime());
						builder.append("<tr><td>").append(
								idTrain + " " + train.getDepartureStation() + " - " + train.getDestinationStation());
						if (train.getName() != null) {
							builder.append("<br>" + "&laquo;" + train.getName() + "&raquo;");
						}
						builder.append("</td>\n<td>" + depTime + "</td>\n<td>"
								+ Util.formatMinutes(trainTravelStayTimes.getDestinationTravelTime()
										- trainTravelStayTimes.getDepartureTravelStayTime())
								+ "</td>\n<td>" + destTime + "</td>\n");
						if (isAllDays) {
							builder.append("<td>\n"
									+ "<form method='get' name='selectDate'>\n<input type=\"date\" name=\"date\">\n"
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
						builder.append("</form>\n" + train.getDepartureDays() + "</td>\n</tr>\n");
					}
					builder.append("</table>\n");
					builder.append(HOME_PAGE_END);
					writeHomePageResponse(builder.toString());
				} else {
					writeHomePageResponse(HOME_PAGE);
				}
			} catch (Throwable t) {
				logger.error(t.getMessage(), t);
			}
		}

		private void writeHomePageResponse(String html) throws Throwable {
			String response = "HTTP/1.1 200 OK\r\n" + "Server: misha-sma-Server/2012\r\n"
					+ "Content-Type: text/html\r\n" + "Connection: close\r\n\r\n";
			String result = response + html;
			os.write(result.getBytes());
			os.flush();
		}

		private void writeFaviconResponse() throws Throwable {
			String response = "HTTP/1.1 200 OK\r\n" + "Server: misha-sma-Server/2012\r\n"
					+ "Content-Type: image/vnd.microsoft.icon\r\n" + "Content-Length: " + FAVICON_BYTES.length + "\r\n"
					+ "Connection: close\r\n\r\n";
			os.write(response.getBytes());
			os.write(FAVICON_BYTES);
			os.flush();
		}

		private String getUrl() throws Throwable {
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
		loadCaches();
		ServerSocket serverSocket = new ServerSocket(PORT);
		logger.info("Server started on port " + PORT + " !!!");
		while (true) {
			Socket socket = serverSocket.accept();
			new Thread(new SocketProcessor(socket)).start();
		}
	}

	private static void loadCaches() {
		CarriageDao.loadCarriageCaches();
		StationDao.loadStationsCaches();
		TrainDao.loadTrainsCache();
	}
}
