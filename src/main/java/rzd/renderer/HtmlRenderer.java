package rzd.renderer;

import rzd.persistence.dao.CarriageDao;
import rzd.persistence.dao.StationDao;
import rzd.persistence.dao.TrainDao;
import rzd.persistence.entity.Carriage;
import rzd.persistence.entity.CarriageSeatNumber;
import rzd.persistence.entity.Train;
import rzd.persistence.entity.User;
import rzd.server.HttpServer;
import rzd.util.Util;

public class HtmlRenderer {
	public static String getHeader(int idDepartureStation, int idDestinationStation, String date) {
		String departureStation = StationDao.STATIONS_ID_NAME_MAP.get(idDepartureStation);
		String destinationStation = StationDao.STATIONS_ID_NAME_MAP.get(idDestinationStation);
		String header = HttpServer.HOME_PAGE_BEGIN.replace("placeholder=\"Откуда\"",
				"value=\"" + departureStation + "\"");
		header = header.replace("placeholder=\"Куда\"", "value=\"" + destinationStation + "\"");
		header = header.replace("value=\"\"", "value=\"" + date + "\"");
		return header;
	}

	public static String getTrainHeader(int idTrain, String date, int delay, int idDestinationStation) {
		StringBuilder builder = new StringBuilder();
		Train train = TrainDao.TRAINS_MAP.get(idTrain);
		String trainNameQuotes = train.getName() == null ? "" : "&laquo;" + train.getName() + "&raquo;";
		builder.append("Поезд №" + idTrain + " " + train.getDepartureStation() + " - " + train.getDestinationStation()
				+ " " + trainNameQuotes + "<br>\n");
		String depTime = Util.addMinutesToDate(train.getDepartureTime(), delay);
		builder.append("Отправление " + Util.convertDateToDots(date) + " " + Util.getDayOfWeek(date) + " в " + depTime
				+ "<br>\n");
		int delayDest = TrainDao.getTravelTime(idTrain, idDestinationStation);
		String[] destDate = Util.calcDestDate(date, depTime, delayDest - delay);
		builder.append("Прибытие " + destDate[0] + " " + destDate[2] + " в " + destDate[1] + "<br>\n");
		builder.append("Время в пути " + Util.formatMinutes(delayDest - delay) + "<br>\n");
		return builder.toString();
	}

	public static String getTrainHeader4Ticket(int idTrain, String date, int delay, int idDepartureStation,
			int idDestinationStation) {
		StringBuilder builder = new StringBuilder();
		String departureStation = StationDao.STATIONS_ID_NAME_MAP.get(idDepartureStation);
		String destinationStation = StationDao.STATIONS_ID_NAME_MAP.get(idDestinationStation);
		Train train = TrainDao.TRAINS_MAP.get(idTrain);
		String trainNameQuotes = train.getName() == null ? "" : "&laquo;" + train.getName() + "&raquo;";
		builder.append("Поезд №" + idTrain + " " + train.getDepartureStation() + " - " + train.getDestinationStation()
				+ " " + trainNameQuotes + "<br>\n");
		String depTime = Util.addMinutesToDate(train.getDepartureTime(), delay);
		builder.append("Отправление " + Util.convertDateToDots(date) + " " + Util.getDayOfWeek(date) + " в " + depTime
				+ " от станции " + departureStation + "<br>\n");
		int delayDest = TrainDao.getTravelTime(idTrain, idDestinationStation);
		String[] destDate = Util.calcDestDate(date, depTime, delayDest - delay);
		builder.append("Прибытие " + destDate[0] + " " + destDate[2] + " в " + destDate[1] + " на станцию "
				+ destinationStation + "<br>\n");
		builder.append("Время в пути " + Util.formatMinutes(delayDest - delay) + "<br>\n");
		return builder.toString();
	}

	public static String getSeatInfo(CarriageSeatNumber carriageSN) {
		StringBuilder builder = new StringBuilder();
		Carriage carriage = carriageSN.getCarriage();
		int seatNumber = carriageSN.getSeatNumber();
		builder.append("Вагон №" + carriage.getCarriageNumber() + " "
				+ CarriageDao.CARRIAGE_NAMES_MAP.get(carriage.getIdCarriageType()) + "<br>\n");
		builder.append(
				"Место " + seatNumber + " " + Util.getSeatType(seatNumber, carriage.getIdCarriageType()) + "<br>\n");
		return builder.toString();
	}

	public static String getUserInfo(User user) {
		StringBuilder builder = new StringBuilder();
		builder.append("Данные пассажира<br>\n");
		builder.append(user.getSurname() + " " + user.getName() + " " + user.getPatronymic() + "<br>\n");
		builder.append("Дата рождения " + Util.addMinutesToDateFull(user.getBirthday(), 0)[0] + "<br>\n");
		builder.append("Паспорт " + user.getIdUser() + "<br>\n");
		builder.append("Email " + user.getEmail() + "<br>\n");
		builder.append("Телефон +" + user.getPhone() + "<br>\n");
		return builder.toString();
	}
}
