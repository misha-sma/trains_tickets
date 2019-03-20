package rzd.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import rzd.persistence.dao.CarriageDao;
import rzd.persistence.dao.SeatDao;
import rzd.persistence.dao.StationDao;
import rzd.persistence.dao.TicketDao;
import rzd.persistence.dao.TrainDao;
import rzd.persistence.dao.UserDao;
import rzd.persistence.entity.SeatsSearchResult;
import rzd.persistence.entity.Train;
import rzd.persistence.entity.User;
import rzd.renderer.SeatsRenderer;
import rzd.util.Util;

public class HttpServer {
	public static final String HOME_PAGE;
	public static final String HOME_PAGE_BEGIN;
	public static final String HOME_PAGE_END;
	public static byte[] faviconBytes;
	public static final Map<Integer, Integer> SEATS_COUNT_MAP = CarriageDao.getSeatsCountMap();
	public static final Map<Integer, String> CARRIAGE_NAMES_MAP = CarriageDao.getCarriageNamesMap();
	public static final String PASSENGER_PAGE;
    public static final String SUCCESS_PAGE;
	public static final int PORT = 8080;
	
	static {
		HOME_PAGE = Util.loadText("web/index.html");
		int endBodyIndex = HOME_PAGE.indexOf("</body>");
		HOME_PAGE_BEGIN = HOME_PAGE.substring(0, endBodyIndex);
		HOME_PAGE_END = HOME_PAGE.substring(endBodyIndex);
        String passengerPage = Util.loadText("web/passenger.html");
        int startIndex = passengerPage.indexOf("Данные пассажира");
        PASSENGER_PAGE = passengerPage.substring(startIndex);
        SUCCESS_PAGE = Util.loadText("web/success.html");
		File file = new File("web/favicon.ico");
		try {
			FileInputStream input = new FileInputStream(file);
			faviconBytes = new byte[(int) file.length()];
			input.read(faviconBytes);
			input.close();
		} catch (FileNotFoundException e) {
			// logger.error(e);
			e.printStackTrace();
		} catch (IOException e) {
			// logger.error(e);
			e.printStackTrace();
		}
	}

	private static class SocketProcessor implements Runnable {
		private Socket socket;
		private InputStream is;
		private OutputStream os;

		private SocketProcessor(Socket socket) throws Throwable {
			this.socket = socket;
			this.is = socket.getInputStream();
			this.os = socket.getOutputStream();
		}

		public void run() {
			try {
				String headers = readInputHeaders();
				String url = getUrl(headers);
				if (url.equals("favicon.ico")) {
					writeFaviconResponse();
				} else if (url.startsWith("?surname=")) {
                  // сохранение данных пассажира
                  try {
                      url = URLDecoder.decode(url, "UTF8");
                  } catch (UnsupportedEncodingException e) {
                      // logger.error(e);
                      e.printStackTrace();
                  }
					System.out.println("url=" + url);
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
					writeHomePageResponse(SUCCESS_PAGE);
				} else if (url.startsWith("?idSeat=")) {
                  // введение данных пассажира
                  try {
                      url = URLDecoder.decode(url, "UTF8");
                  } catch (UnsupportedEncodingException e) {
                      // logger.error(e);
                      e.printStackTrace();
                  }
                  System.out.println("url=" + url);
                  Map<String, String> params = Util.parseParameters(url);
                  long idSeat=Long.parseLong(params.get("idSeat"));
                  int idDepartureStation=Integer.parseInt(params.get("from"));
                  int idDestinationStation=Integer.parseInt(params.get("to"));
                  String date=SeatDao.getDepartureDateByIdSeat(idSeat, idDepartureStation); 
                  StringBuilder builder=new StringBuilder();
                  String header=getHeader(idDepartureStation, idDestinationStation, date);
                  builder.append(header);
                  int idTrain = SeatDao.getIdTrainByIdSeat(idSeat);
                  int delay = TrainDao.getTravelStayTime(idTrain, idDepartureStation);
                  String trainHeader=getTrainHeader(idTrain, date, delay, idDestinationStation);
                  builder.append(trainHeader);
                  builder.append("\n<br>\n");
                  
                  
                  String passengerPage = PASSENGER_PAGE.replace("name=\"from\" value=\"\">", "name=\"from\" value=\""+idDepartureStation+"\">");
                  passengerPage = passengerPage.replace("name=\"to\" value=\"\">", "name=\"to\" value=\""+idDestinationStation+"\">");
                  passengerPage = passengerPage.replace("name=\"idSeat\" value=\"\">", "name=\"idSeat\" value=\""+idSeat+"\">");
                  builder.append(passengerPage);
                  writeHomePageResponse(builder.toString());
				} else if (url.startsWith("?date=")) {
					// выбор места
					try {
						url = URLDecoder.decode(url, "UTF8");
					} catch (UnsupportedEncodingException e) {
						// logger.error(e);
						e.printStackTrace();
					}
					System.out.println("url=" + url);
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
                    String header=getHeader(idDepartureStation, idDestinationStation, date);
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
                    String trainHeader=getTrainHeader(idTrain, date, delay, idDestinationStation);
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
					try {
						url = URLDecoder.decode(url, "UTF8");
					} catch (UnsupportedEncodingException e) {
						// logger.error(e);
						e.printStackTrace();
					}
					System.out.println("url=" + url);
					Map<String, String> params = Util.parseParameters(url);
					String departureStation = params.get("from");
					String destinationStation = params.get("to");
					int idDepartureStation = TrainDao.getIdStation(departureStation);
					int idDestinationStation = TrainDao.getIdStation(destinationStation);
					String date = params.get("date");
					StringBuilder builder = new StringBuilder();
					String header = HOME_PAGE_BEGIN.replace("placeholder=\"Откуда\"",
							"value=\"" + departureStation + "\"");
					header = header.replace("placeholder=\"Куда\"", "value=\"" + destinationStation + "\"");
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
					List<Integer> trains = null;
					if (isAllDays) {
						trains = TrainDao.getTrainsByStations(idDepartureStation, idDestinationStation);
					} else {
						trains = TrainDao.getTrainsByStationsAndDate(idDepartureStation, idDestinationStation, date);
					}

					for (int idTrain : trains) {
						String trainDepartureStation = TrainDao.getDepartureStation(idTrain);
						String trainDestinationStation = TrainDao.getDestinationStation(idTrain);
						Train train = TrainDao.getTrainById(idTrain);
						int departureTravelTime = TrainDao.getTravelStayTime(idTrain, idDepartureStation);
						int destinationTravelTime = TrainDao.getTravelTime(idTrain, idDestinationStation);
						String depTime = Util.addMinutesToDate(train.getDepartureTime(), departureTravelTime);
						String destTime = Util.addMinutesToDate(train.getDepartureTime(), destinationTravelTime);
						builder.append("<tr><td>").append(
								train.getIdTrain() + " " + trainDepartureStation + " - " + trainDestinationStation);
						if (train.getName() != null && !train.getName().isEmpty()) {
							builder.append("<br>" + "&laquo;"+train.getName()+"&raquo;");
						}
						builder.append("</td>\n<td>" + depTime + "</td>\n<td>"
								+ Util.formatMinutes(destinationTravelTime - departureTravelTime) + "</td>\n<td>"
								+ destTime + "</td>\n");
						if (isAllDays) {
							builder.append("<td>\n"
									+ "<form method='get' name='selectDate'>\n<input type=\"date\" name=\"date\">\n"
									+ "<input type=\"hidden\" name=\"idTrain\" value=\"" + idTrain + "\" />"
									+ "<input type=\"hidden\" name=\"from\" value=\"" + idDepartureStation + "\" />"
									+ "<input type=\"hidden\" name=\"to\" value=\"" + idDestinationStation + "\" />"
									+ "<input type=\"submit\" name=\"submitDateButton\" value=\"Выбрать\">");
						} else {
							builder.append("<td>\n<form method='get' name='selectSeat'>\n"
									+ "<input type=\"hidden\" name=\"date\" value=\"" + date + "\" />"
									+ "<input type=\"hidden\" name=\"idTrain\" value=\"" + idTrain + "\" />"
									+ "<input type=\"hidden\" name=\"from\" value=\"" + idDepartureStation + "\" />"
									+ "<input type=\"hidden\" name=\"to\" value=\"" + idDestinationStation + "\" />"
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
				// logger.error(t);
				t.printStackTrace();
			} finally {
				try {
					socket.close();
				} catch (Throwable t) {
					// logger.error(t);
					t.printStackTrace();
				}
			}
			// logger.info("----------------------Client processing
			// finished---------------------------");
			System.out.println("----------------------Client processing finished---------------------------");
		}

		private void writeHomePageResponse(String html) throws Throwable {
            String response = "HTTP/1.1 200 OK\r\n" + "Server: misha-sma-Server/2012\r\n" + "Content-Type: text/html\r\n"
                + "Connection: close\r\n\r\n";
			String result = response + html;
			os.write(result.getBytes());
			os.flush();
		}

		private void writeFaviconResponse() throws Throwable {
			String response = "HTTP/1.1 200 OK\r\n" + "Server: misha-sma-Server/2012\r\n"
					+ "Content-Type: image/vnd.microsoft.icon\r\n" + "Content-Length: " + faviconBytes.length + "\r\n"
					+ "Connection: close\r\n\r\n";
			os.write(response.getBytes());
			os.write(faviconBytes);
			os.flush();
		}

		private String readInputHeaders() throws Throwable {
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null && !line.trim().isEmpty()) {
				// logger.debug("line=" + line);
				System.out.println("line=" + line);
				builder.append(line).append('\n');
			}
			return builder.toString();
		}
	}

	public static void main(String[] args) throws Throwable {
		ServerSocket serverSocket = new ServerSocket(PORT);
		// logger.info("Server started!!!");
		System.out.println("Server started!!!");
		while (true) {
			Socket socket = serverSocket.accept();
			// logger.info("---------------Client accepted------------------------");
			System.out.println("---------------Client accepted------------------------");
			new Thread(new SocketProcessor(socket)).start();
		}
	}

	public static String getUrl(String headers) {
		String url = "";
		StringTokenizer tokenizer = new StringTokenizer(headers);
		while (tokenizer.hasMoreTokens()) {
			String word = tokenizer.nextToken();
			if (word.equals("GET")) {
				url = tokenizer.nextToken();
			}
		}
		url = url.startsWith("/") ? url.substring(1) : url;
		url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
		return url;
	}
	
  private static String getHeader(int idDepartureStation, int idDestinationStation, String date) {
    String departureStation = StationDao.getStationNameById(idDepartureStation);
    String destinationStation = StationDao.getStationNameById(idDestinationStation);
    String header = HOME_PAGE_BEGIN.replace("placeholder=\"Откуда\"", "value=\"" + departureStation + "\"");
    header = header.replace("placeholder=\"Куда\"", "value=\"" + destinationStation + "\"");
    header = header.replace("value=\"\"", "value=\"" + date + "\"");
    return header;
  }

  private static String getTrainHeader(int idTrain, String date, int delay, int idDestinationStation) {
    StringBuilder builder = new StringBuilder();
    String departureStationTrain = TrainDao.getDepartureStation(idTrain);
    String destinationStationTrain = TrainDao.getDestinationStation(idTrain);
    Train train = TrainDao.getTrainById(idTrain);
    String trainNameQuotes = train.getName() == null ? "" : "&laquo;" + train.getName() + "&raquo;";
    builder.append("Поезд №" + idTrain + " " + departureStationTrain + " - " + destinationStationTrain + " "
        + trainNameQuotes + "<br>");
    String depTime = Util.addMinutesToDate(train.getDepartureTime(), delay);
    builder.append(
        "Отправление " + Util.convertDateToDots(date) + " " + Util.getDayOfWeek(date) + " в " + depTime + "<br>");
    int delayDest = TrainDao.getTravelTime(idTrain, idDestinationStation);
    String[] destDate = Util.calcDestDate(date, depTime, delayDest - delay);
    builder.append("Прибытие " + destDate[0] + " " + destDate[2] + " в " + destDate[1] + "<br>");
    builder.append("Время в пути "+Util.formatMinutes(delayDest-delay)+"<br>");
    return builder.toString();
  }
}
