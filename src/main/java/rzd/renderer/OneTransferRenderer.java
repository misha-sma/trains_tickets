package rzd.renderer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import rzd.persistence.dao.StationDao;
import rzd.persistence.dao.TrainDao;
import rzd.persistence.entity.OneTransferTrain4UI;
import rzd.persistence.entity.StayTimeWithDepartureDays;
import rzd.persistence.entity.Train;
import rzd.persistence.entity.TrainTravelStayTimesOneTransfer;
import rzd.util.DateUtil;
import rzd.util.Util;

public class OneTransferRenderer {
	public static String oneTransferRoutesSearch(int idDepartureStation, int idDestinationStation) {
		List<TrainTravelStayTimesOneTransfer> trains = TrainDao.getTrainsByStationsOneTransfer(idDepartureStation,
				idDestinationStation);
		List<OneTransferTrain4UI> rows = new ArrayList<OneTransferTrain4UI>();
		for (TrainTravelStayTimesOneTransfer trainTravelStayTimes : trains) {
			int idTrainFrom = trainTravelStayTimes.getIdTrainFrom();
			Train trainFrom = TrainDao.TRAINS_MAP.get(idTrainFrom);
			String depTimeFrom = DateUtil.addMinutesToDate(trainFrom.getDepartureTime(),
					trainTravelStayTimes.getTravelStayTimeFrom());
			String destTimeFrom = DateUtil.addMinutesToDate(trainFrom.getDepartureTime(),
					trainTravelStayTimes.getTravelTimeFrom());
			StringBuilder builder = new StringBuilder();
			builder.append("<tr><td>")
					.append("<a href=\"?idTrain=" + idTrainFrom + "&from=" + idDepartureStation + "&to="
							+ trainTravelStayTimes.getTransferIdStation() + "\">" + idTrainFrom + " "
							+ trainFrom.getDepartureStation() + " - " + trainFrom.getDestinationStation() + "</a>");
			if (trainFrom.getName() != null) {
				builder.append("<br>" + "&laquo;" + trainFrom.getName() + "&raquo;");
			}
			builder.append("</td>\n<td>" + depTimeFrom + "</td>\n<td>"
					+ DateUtil.formatMinutes(
							trainTravelStayTimes.getTravelTimeFrom() - trainTravelStayTimes.getTravelStayTimeFrom())
					+ "</td>\n<td>" + destTimeFrom + "</td>\n");

			// ----------------------------------------------------

			String transferStationName = StationDao.STATIONS_ID_NAME_MAP
					.get(trainTravelStayTimes.getTransferIdStation());
			builder.append("<td>" + transferStationName + "</td>\n");

			int idTrainTo = trainTravelStayTimes.getIdTrainTo();
			Train trainTo = TrainDao.TRAINS_MAP.get(idTrainTo);

			StayTimeWithDepartureDays stayTimeWithDD = getStayTime(trainFrom, trainTo,
					trainTravelStayTimes.getTravelTimeFrom(), trainTravelStayTimes.getTravelStayTimeTo(),
					trainTravelStayTimes.getTravelStayTimeFrom());
			builder.append("<td>" + DateUtil.formatMinutes(stayTimeWithDD.getStayTimeMinutes()) + "</td>\n");

			// ----------------------------------------------------

			String depTimeTo = DateUtil.addMinutesToDate(trainTo.getDepartureTime(),
					trainTravelStayTimes.getTravelStayTimeTo());
			String destTimeTo = DateUtil.addMinutesToDate(trainTo.getDepartureTime(),
					trainTravelStayTimes.getTravelTimeTo());
			builder.append("<td>")
					.append("<a href=\"?idTrain=" + idTrainTo + "&from=" + trainTravelStayTimes.getTransferIdStation()
							+ "&to=" + idDestinationStation + "\">" + idTrainTo + " " + trainTo.getDepartureStation()
							+ " - " + trainTo.getDestinationStation() + "</a>");
			if (trainTo.getName() != null) {
				builder.append("<br>" + "&laquo;" + trainTo.getName() + "&raquo;");
			}
			builder.append("</td>\n<td>" + depTimeTo + "</td>\n<td>"
					+ DateUtil.formatMinutes(
							trainTravelStayTimes.getTravelTimeTo() - trainTravelStayTimes.getTravelStayTimeTo())
					+ "</td>\n<td>" + destTimeTo + "</td>\n");

			// ----------------------------------------------------

			int totalTimeMinutes = trainTravelStayTimes.getTravelTimeFrom()
					- trainTravelStayTimes.getTravelStayTimeFrom() + stayTimeWithDD.getStayTimeMinutes()
					+ trainTravelStayTimes.getTravelTimeTo() - trainTravelStayTimes.getTravelStayTimeTo();
			builder.append("<td>" + DateUtil.formatMinutes(totalTimeMinutes) + "</td>\n");
			builder.append("<td>" + Util.weekDaysToString(stayTimeWithDD.getDepartureDays()) + "</td>\n</tr>\n");
			rows.add(new OneTransferTrain4UI(builder.toString(), totalTimeMinutes));
		}
		Collections.sort(rows);
		StringBuilder builder = new StringBuilder();
		for (OneTransferTrain4UI row : rows) {
			builder.append(row.getHtml());
		}
		return builder.toString();
	}

	private static StayTimeWithDepartureDays getStayTime(Train trainFrom, Train trainTo, int travelTimeFrom,
			int travelStayTimeTo, int travelStayTimeFrom) {
		Map<String, Integer> trainFromDestMap = getTrainDestMap(trainFrom, travelTimeFrom);
		Map<String, Integer> trainToDestMap = getTrainDestMap(trainTo, travelStayTimeTo);
		completeTrainDestMap(trainToDestMap);
		int minDelta = Integer.MAX_VALUE;
		List<String> minDays = new LinkedList<String>();
		for (String weekDayFrom : trainFromDestMap.keySet()) {
			int weekMinutesFrom = trainFromDestMap.get(weekDayFrom);
			for (String weekDayTo : trainToDestMap.keySet()) {
				int weekMinutesTo = trainToDestMap.get(weekDayTo);
				if (weekMinutesTo <= weekMinutesFrom) {
					continue;
				}
				int delta = weekMinutesTo - weekMinutesFrom;
				if (delta < minDelta) {
					minDelta = delta;
					minDays = new LinkedList<String>();
					minDays.add(weekDayFrom);
				} else if (delta == minDelta) {
					minDays.add(weekDayFrom);
				}
			}
		}
		return new StayTimeWithDepartureDays(minDelta,
				DateUtil.getWeekDaysTrue(minDays, travelStayTimeFrom, trainFrom.getDepartureTime()));
	}

	private static Map<String, Integer> getTrainDestMap(Train train, int travelTime) {
		Map<String, Integer> trainDestMap = new HashMap<String, Integer>();
		String depDays = train.getDepartureDays();
		Calendar calendarDep = Calendar.getInstance();
		calendarDep.setTime(train.getDepartureTime());
		calendarDep.add(Calendar.MINUTE, travelTime);
		int deltaDays = calendarDep.get(Calendar.DAY_OF_MONTH) - 1;
		int hours = calendarDep.get(Calendar.HOUR_OF_DAY);
		int minutes = calendarDep.get(Calendar.MINUTE);
		String[] parts = depDays.equals("ежд") ? DateUtil.DAYS_OF_WEEK_ARRAY : depDays.split(",");
		for (String part : parts) {
			int dayOfWeekInt = DateUtil.DAY_OF_WEEK_MAP_REVERSE.get(part);
			dayOfWeekInt += deltaDays;
			dayOfWeekInt = dayOfWeekInt % 7;
			dayOfWeekInt = dayOfWeekInt == 0 ? 7 : dayOfWeekInt;
			int weekMinutes = DateUtil.DAY_OF_WEEK_MINUTES_MAP.get(dayOfWeekInt) + hours * 60 + minutes;
			trainDestMap.put(part, weekMinutes);
		}
		return trainDestMap;
	}

	private static void completeTrainDestMap(Map<String, Integer> trainDestMap) {
		int minMinutes = Integer.MAX_VALUE;
		for (String weekDay : trainDestMap.keySet()) {
			int weekMinutes = trainDestMap.get(weekDay);
			if (weekMinutes < minMinutes) {
				minMinutes = weekMinutes;
			}
		}
		trainDestMap.put("вспн", minMinutes + 7 * 1440);
	}

}
