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
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import rzd.persistence.dao.TrainDao;
import rzd.persistence.entity.Train;
import rzd.util.Util;

public class HttpServer {
  public static final String HOME_PAGE;
  public static final String HOME_PAGE_BEGIN;
  public static final String HOME_PAGE_END;
  public static byte[] faviconBytes;

  public static int HITS_COUNT = 10;

  static {
      HOME_PAGE = Util.loadText("web/index.html");
      int endBodyIndex = HOME_PAGE.indexOf("</body>");
      HOME_PAGE_BEGIN = HOME_PAGE.substring(0, endBodyIndex);
      HOME_PAGE_END = HOME_PAGE.substring(endBodyIndex);
      File file = new File("web/favicon.ico");
      try {
          FileInputStream input = new FileInputStream(file);
          faviconBytes = new byte[(int) file.length()];
          input.read(faviconBytes);
          input.close();
      } catch (FileNotFoundException e) {
//          logger.error(e);
        e.printStackTrace();
      } catch (IOException e) {
//          logger.error(e);
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
              } else if (url.startsWith("?from=")) {
                try {
                  url = URLDecoder.decode(url, "UTF8");
              } catch (UnsupportedEncodingException e) {
                  //logger.error(e);
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
          builder.append(HOME_PAGE_BEGIN);
          builder.append("<table border=\"1\">\n");
          builder.append(
              "<tr>\n<th>Поезд</th>\n<th>Время отправления</th>\n<th>Время в пути</th>\n<th>Время прибытия</th>\n<th></th>\n</tr>\n");
          if (date == null || date.isEmpty()) {
            List<Integer> trains = TrainDao.getTrainsByStations(idDepartureStation, idDestinationStation);
            for (int idTrain : trains) {
              String trainDepartureStation = TrainDao.getDepartureStation(idTrain);
              String trainDestinationStation = TrainDao.getDestinationStation(idTrain);
              Train train = TrainDao.getTrainById(idTrain);
              int departureTravelTime = TrainDao.getTravelStayTime(idTrain, idDepartureStation);
              int destinationTravelTime = TrainDao.getTravelTime(idTrain, idDestinationStation);

              Calendar calendarDep = Calendar.getInstance();
              calendarDep.setTime(train.getDepartureTime());
              calendarDep.add(Calendar.MINUTE, departureTravelTime);
              String depTime = "" + calendarDep.get(Calendar.HOUR) + ":" + calendarDep.get(Calendar.MINUTE);

              Calendar calendarDest = Calendar.getInstance();
              calendarDest.setTime(train.getDepartureTime());
              calendarDest.add(Calendar.MINUTE, destinationTravelTime);
              String destTime = "" + calendarDest.get(Calendar.HOUR) + ":" + calendarDest.get(Calendar.MINUTE);

              builder.append("<tr><td>")
                  .append(train.getIdTrain() + " " + trainDepartureStation + " - " + trainDestinationStation);
              if (train.getName() != null && !train.getName().isEmpty()) {
                builder.append("<br>" + train.getName());
              }
              builder.append("</td>\n<td>" + depTime + "</td>\n<td>" + (destinationTravelTime - departureTravelTime)
                  + "</td>\n<td>" + destTime + "</td>\n");
              builder.append(
                  "<td>\n<form method='get' name='selectDate'>\n<input type=\"submit\" name=\"selectDateButton\" value=\"Выбрать дату\">\n");
              builder.append("</form>\n" + train.getDepartureDays() + "</td>\n</tr>\n");
            }
            builder.append("</table>\n");
          } else {
            List<Integer> trains = TrainDao.getTrainsByStationsAndDate(idDepartureStation, idDestinationStation, date);

          }
          builder.append(HOME_PAGE_END);
          writeHomePageResponse(builder.toString());
        } else {
          writeHomePageResponse(HOME_PAGE);
        }
          } catch (Throwable t) {
//              logger.error(t);
            t.printStackTrace();
          } finally {
              try {
                  socket.close();
              } catch (Throwable t) {
//                  logger.error(t);
                t.printStackTrace();
              }
          }
//          logger.info("----------------------Client processing finished---------------------------");
      System.out.println("----------------------Client processing finished---------------------------");
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
//              logger.debug("line=" + line);
            System.out.println("line=" + line);  
            builder.append(line).append('\n');
          }
          return builder.toString();
      }
  }

  public static void main(String[] args) throws Throwable {
      ServerSocket serverSocket = new ServerSocket(8080);
      //logger.info("Server started!!!");
      System.out.println("Server started!!!");
      while (true) {
          Socket socket = serverSocket.accept();
          //logger.info("---------------Client accepted------------------------");
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
}
