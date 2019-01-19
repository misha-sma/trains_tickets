package rzd.renderer;

import java.util.Map;

import rzd.persistence.dao.SeatDao;
import rzd.server.HttpServer;

public class SeatsRenderer {
	private static final String TABLE_GAP = "<td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>\n";
	private static final String COUPE_GAP = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
	private static final String DOUBLE_DECKER_GAP = "&nbsp;&nbsp;&nbsp;";

	public static String renderCarriage(int carriageNumber, int carriageType, Map<Integer, Long> seatsMap,
			int idDepartureStation, int idDestinationStation) {
		switch (carriageType) {
		case 1:
			// плацкарт
			return renderEconom(carriageNumber, carriageType, seatsMap, idDepartureStation, idDestinationStation);
		case 2:
			// купе
			return renderCoupe(carriageNumber, carriageType, seatsMap, idDepartureStation, idDestinationStation);
		case 3:
			// СВ
			return renderCB(carriageNumber, carriageType, seatsMap, idDepartureStation, idDestinationStation);
		case 4:
			// сидячий
			return renderSeating(carriageNumber, carriageType, seatsMap, idDepartureStation, idDestinationStation);
		case 5:
			// двухэтажный
			return renderDoubleDecker(carriageNumber, carriageType, seatsMap, idDepartureStation, idDestinationStation);
		}
		return "";
	}

	private static String renderDoubleDecker(int carriageNumber, int carriageType, Map<Integer, Long> seatsMap,
			int idDepartureStation, int idDestinationStation) {
		// int seatsCount = HttpServer.SEATS_COUNT_MAP.get(carriageType);
		String carriageName = HttpServer.CARRIAGE_NAMES_MAP.get(carriageType);
		StringBuilder builder = new StringBuilder();
		builder.append("<table border=\"0\">\n");
		builder.append("<caption>Вагон номер " + carriageNumber + " " + carriageName + "</caption>\n");
		builder.append("<tr><td>2 этаж верхние" + DOUBLE_DECKER_GAP + "</td>\n");
		for (int seatNumber = 34; seatNumber <= 64; seatNumber += 2) {
			builder.append(addSeat(carriageNumber, seatNumber, seatsMap, idDepartureStation, idDestinationStation));
			if (seatNumber % 4 == 0 && seatNumber < 64) {
				builder.append(TABLE_GAP);
			}
		}

		builder.append("</tr>\n<tr><td>2 этаж нижние" + DOUBLE_DECKER_GAP + "</td>\n");
		for (int seatNumber = 33; seatNumber <= 63; seatNumber += 2) {
			builder.append(addSeat(carriageNumber, seatNumber, seatsMap, idDepartureStation, idDestinationStation));
			if ((seatNumber + 1) % 4 == 0 && seatNumber < 63) {
				builder.append(TABLE_GAP);
			}
		}

		builder.append("</tr>\n<tr><td>1 этаж верхние" + DOUBLE_DECKER_GAP + "</td>\n");
		for (int seatNumber = 2; seatNumber <= 32; seatNumber += 2) {
			builder.append(addSeat(carriageNumber, seatNumber, seatsMap, idDepartureStation, idDestinationStation));
			if (seatNumber % 4 == 0 && seatNumber < 32) {
				builder.append(TABLE_GAP);
			}
		}

		builder.append("</tr>\n<tr><td>1 этаж нижние" + DOUBLE_DECKER_GAP + "</td>\n");
		for (int seatNumber = 1; seatNumber <= 31; seatNumber += 2) {
			builder.append(addSeat(carriageNumber, seatNumber, seatsMap, idDepartureStation, idDestinationStation));
			if ((seatNumber + 1) % 4 == 0 && seatNumber < 31) {
				builder.append(TABLE_GAP);
			}
		}
		builder.append("</tr>\n</table><br>\n");
		return builder.toString();
	}

	private static String renderSeating(int carriageNumber, int carriageType, Map<Integer, Long> seatsMap,
			int idDepartureStation, int idDestinationStation) {
		// int seatsCount = HttpServer.SEATS_COUNT_MAP.get(carriageType);
		String carriageName = HttpServer.CARRIAGE_NAMES_MAP.get(carriageType);
		StringBuilder builder = new StringBuilder();
		builder.append("<table border=\"0\">\n");
		builder.append("<caption>Вагон номер " + carriageNumber + " " + carriageName + "</caption>\n");
		builder.append("<tr><td></td>\n");
		builder.append(addSeat(carriageNumber, 1, seatsMap, idDepartureStation, idDestinationStation));
		for (int seatNumber = 3; seatNumber <= 63; seatNumber += 4) {
			builder.append(TABLE_GAP);
			builder.append(addSeat(carriageNumber, seatNumber, seatsMap, idDepartureStation, idDestinationStation));
		}

		builder.append("</tr>\n<tr><td></td>\n");
		builder.append(addSeat(carriageNumber, 2, seatsMap, idDepartureStation, idDestinationStation));
		for (int seatNumber = 4; seatNumber <= 64; seatNumber += 4) {
			builder.append(TABLE_GAP);
			builder.append(addSeat(carriageNumber, seatNumber, seatsMap, idDepartureStation, idDestinationStation));
		}

		builder.append("</tr>\n<tr><td></td>\n");
		for (int seatNumber = 6; seatNumber <= 66; seatNumber += 4) {
			builder.append(TABLE_GAP);
			builder.append(addSeat(carriageNumber, seatNumber, seatsMap, idDepartureStation, idDestinationStation));
		}

		builder.append("</tr>\n<tr><td></td>\n");
		for (int seatNumber = 5; seatNumber <= 65; seatNumber += 4) {
			builder.append(TABLE_GAP);
			builder.append(addSeat(carriageNumber, seatNumber, seatsMap, idDepartureStation, idDestinationStation));
		}
		builder.append("</tr>\n</table><br>\n");
		return builder.toString();
	}

	private static String renderCB(int carriageNumber, int carriageType, Map<Integer, Long> seatsMap,
			int idDepartureStation, int idDestinationStation) {
		// int seatsCount = HttpServer.SEATS_COUNT_MAP.get(carriageType);
		String carriageName = HttpServer.CARRIAGE_NAMES_MAP.get(carriageType);
		StringBuilder builder = new StringBuilder();
		builder.append("<table border=\"0\">\n");
		builder.append("<caption>Вагон номер " + carriageNumber + " " + carriageName + "</caption>\n");
		builder.append("<tr><td>нижние" + COUPE_GAP + "</td>\n");
		for (int seatNumber = 1; seatNumber <= 18; ++seatNumber) {
			builder.append(addSeat(carriageNumber, seatNumber, seatsMap, idDepartureStation, idDestinationStation));
			if (seatNumber % 2 == 0 && seatNumber < 18) {
				builder.append(TABLE_GAP);
			}
		}
		builder.append("</tr>\n</table><br>\n");
		return builder.toString();
	}

	private static String renderCoupe(int carriageNumber, int carriageType, Map<Integer, Long> seatsMap,
			int idDepartureStation, int idDestinationStation) {
		// int seatsCount = HttpServer.SEATS_COUNT_MAP.get(carriageType);
		String carriageName = HttpServer.CARRIAGE_NAMES_MAP.get(carriageType);
		StringBuilder builder = new StringBuilder();
		builder.append("<table border=\"0\">\n");
		builder.append("<caption>Вагон номер " + carriageNumber + " " + carriageName + "</caption>\n");
		builder.append("<tr><td>верхние" + COUPE_GAP + "</td>\n");
		for (int seatNumber = 2; seatNumber <= 36; seatNumber += 2) {
			builder.append(addSeat(carriageNumber, seatNumber, seatsMap, idDepartureStation, idDestinationStation));
			if (seatNumber % 4 == 0 && seatNumber < 36) {
				builder.append(TABLE_GAP);
			}
		}

		builder.append("</tr>\n<tr><td>нижние" + COUPE_GAP + "</td>\n");
		for (int seatNumber = 1; seatNumber <= 35; seatNumber += 2) {
			builder.append(addSeat(carriageNumber, seatNumber, seatsMap, idDepartureStation, idDestinationStation));
			if ((seatNumber + 1) % 4 == 0 && seatNumber < 35) {
				builder.append(TABLE_GAP);
			}
		}
		builder.append("</tr>\n</table><br>\n");
		return builder.toString();
	}

	private static String renderEconom(int carriageNumber, int carriageType, Map<Integer, Long> seatsMap,
			int idDepartureStation, int idDestinationStation) {
		// int seatsCount = HttpServer.SEATS_COUNT_MAP.get(carriageType);
		String carriageName = HttpServer.CARRIAGE_NAMES_MAP.get(carriageType);
		StringBuilder builder = new StringBuilder();
		builder.append("<table border=\"0\">\n");
		builder.append("<caption>Вагон номер " + carriageNumber + " " + carriageName + "</caption>\n");
		builder.append("<tr><td>верхние</td>\n");
		for (int seatNumber = 2; seatNumber <= 36; seatNumber += 2) {
			builder.append(addSeat(carriageNumber, seatNumber, seatsMap, idDepartureStation, idDestinationStation));
			if (seatNumber % 4 == 0 && seatNumber < 36) {
				builder.append(TABLE_GAP);
			}
		}

		builder.append("</tr>\n<tr><td>нижние</td>\n");
		for (int seatNumber = 1; seatNumber <= 35; seatNumber += 2) {
			builder.append(addSeat(carriageNumber, seatNumber, seatsMap, idDepartureStation, idDestinationStation));
			if ((seatNumber + 1) % 4 == 0 && seatNumber < 35) {
				builder.append(TABLE_GAP);
			}
		}

		builder.append("</tr>\n<tr><td>боковые верхние</td>\n");
		for (int seatNumber = 54; seatNumber >= 38; seatNumber -= 2) {
			builder.append("<td></td>\n");
			builder.append(addSeat(carriageNumber, seatNumber, seatsMap, idDepartureStation, idDestinationStation));
			if (seatNumber > 38) {
				builder.append(TABLE_GAP);
			}
		}

		builder.append("</tr>\n<tr><td>боковые нижние</td>\n");
		for (int seatNumber = 53; seatNumber >= 37; seatNumber -= 2) {
			builder.append("<td></td>\n");
			builder.append(addSeat(carriageNumber, seatNumber, seatsMap, idDepartureStation, idDestinationStation));
			if (seatNumber > 37) {
				builder.append(TABLE_GAP);
			}
		}
		builder.append("</tr>\n</table><br>\n");
		return builder.toString();
	}

	private static String addSeat(int carriageNumber, int seatNumber, Map<Integer, Long> seatsMap,
			int idDepartureStation, int idDestinationStation) {
		int key = carriageNumber * SeatDao.SEATS_HASH_BASE + seatNumber;
		Long idSeat = seatsMap.get(key);
		if (idSeat == null) {
			return "<td>x</td>\n";
		}
		return "<td><a href=\"?idSeat=" + idSeat + "&from=" + idDepartureStation + "&to=" + idDestinationStation + "\">"
				+ seatNumber + "</a></td>\n";
	}
}
