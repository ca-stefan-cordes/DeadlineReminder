package de.quaddy_services.deadlinereminder.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.quaddy_services.deadlinereminder.Deadline;
import de.quaddy_services.deadlinereminder.Storage;

public class FileStorage implements Storage {
	private static final Logger LOGGER = LoggerFactory.getLogger(FileStorage.class);

	public static final String TERMIN_TXT = "termin.txt";
	private static final String INFO_PREFIX = "--";
	private static final String INFO_PREFIX2 = "#";
	public static final String TERMIN_DONE_TXT = "termin-done.txt";
	private static DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
	private static DateFormat timeFormat = new SimpleDateFormat("HH:mm");

	private File getDirectory() {
		File tempUserHome = new File(System.getProperty("user.home"));
		File tempDir = new File(tempUserHome.getAbsolutePath() + "/" + "DeadlineReminder");
		tempDir.mkdirs();
		return tempDir;
	}

	@Override
	public String getSourceInfo() {
		return getDirectory().getAbsolutePath() + "/" + TERMIN_TXT;
	}

	@Override
	public List<Deadline> getOpenDeadlines(Date to) {
		try {
			List<Deadline> tempDeadlines = readDeadlines(to, TERMIN_TXT);
			List<Deadline> tempDoneDeadlines = readDeadlines(null, TERMIN_DONE_TXT);
			List<Deadline> tempFound = new ArrayList<Deadline>(tempDeadlines);
			tempFound.removeAll(tempDoneDeadlines);
			return tempFound;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private List<Deadline> readDeadlines(Date to, String tempFileName) throws FileNotFoundException, IOException {
		List<Deadline> tempMatchingDeadlines = new ArrayList<Deadline>();
		File tempFile = new File(getDirectory().getAbsolutePath() + "/" + tempFileName);
		if (!tempFile.exists()) {
			return tempMatchingDeadlines;
		}
		BufferedReader tempReader = new BufferedReader(createReader(tempFile));
		String tempLine;
		while (null != (tempLine = tempReader.readLine())) {
			List<Deadline> tempDeadlines = parseDeadline(tempLine, to != null);
			for (Deadline tempDeadline : tempDeadlines) {
				if (tempDeadline != null && (to == null || to.after(tempDeadline.getWhen()))) {
					tempMatchingDeadlines.add(tempDeadline);
				}
			}
		}
		tempReader.close();
		return tempMatchingDeadlines;
	}

	protected Reader createReader(File tempFile) throws FileNotFoundException {
		return new FileReader(tempFile);
	}

	private List<Deadline> parseDeadline(String tempLine, boolean aRepeating) {
		List<Deadline> tempDeadlines = new ArrayList<Deadline>();
		if (tempLine.startsWith(INFO_PREFIX)) {
			return tempDeadlines;
		}
		if (tempLine.startsWith(INFO_PREFIX2)) {
			return tempDeadlines;
		}
		try {
			String tempDateChars = tempLine.substring(0, 10);
			tempDateChars = tempDateChars.replace('?', '0');
			if (tempDateChars.endsWith("0000")) {
				Calendar tempCal = Calendar.getInstance();
				int tempCurrentYear = tempCal.get(Calendar.YEAR) - 1;
				tempDateChars = tempDateChars.substring(0, tempDateChars.length() - 4) + tempCurrentYear;
			}
			Date tempDate = dateFormat.parse(tempDateChars);
			String tempInfo = tempLine.substring(10);
			if (tempInfo.startsWith("*") && aRepeating) {
				// repeating
				addRepeating(tempDeadlines, tempDate, tempInfo);
			} else {
				Deadline tempDeadline = new Deadline();
				tempDate = addTime(tempDate, tempInfo);
				tempDeadline.setWhen(tempDate);
				tempDeadline.setInfo(tempInfo);
				tempDeadlines.add(tempDeadline);
			}
		} catch (Exception e) {
			// e.printStackTrace();
			if (tempLine.trim().length() > 0) {
				Deadline tempDeadline = new Deadline();
				tempDeadline.setWhen(new Date());
				tempDeadline.setInfo(tempLine);
				tempDeadlines.add(tempDeadline);
			}

		}
		return tempDeadlines;
	}

	/**
	 * Try to add a time. e.g. *1w 17:00 David Nachhilfe
	 * 
	 * @param aDate
	 * @param aInfo
	 * @return
	 */
	private Date addTime(Date aDate, String aInfo) {
		Date tempDate = aDate;
		Date tempTime = null;
		StringTokenizer tempTokens = new StringTokenizer(aInfo, " ");
		while (tempTokens.hasMoreTokens()) {
			String tempToken = tempTokens.nextToken();
			if (tempToken.length() > 3 && Character.isDigit(tempToken.charAt(0))) {
				try {
					tempTime = timeFormat.parse(tempToken);
					break;
				} catch (ParseException e) {
					// ignore
				}
			}
		}
		if (tempTime == null) {
			return aDate;
			//			try {
			//				tempTime = timeFormat.parse("08:00");
			//			} catch (ParseException e) {
			//				// ignore
			//			}
		}
		Calendar tempCal = Calendar.getInstance();
		tempCal.setTime(tempDate);
		Calendar tempTimeCal = Calendar.getInstance();
		tempTimeCal.setTime(tempTime);
		tempCal.add(Calendar.HOUR_OF_DAY, tempTimeCal.get(Calendar.HOUR_OF_DAY));
		tempCal.add(Calendar.MINUTE, tempTimeCal.get(Calendar.MINUTE));
		tempDate = tempCal.getTime();

		return tempDate;
	}

	private class UnitAndStep {
		int unit;
		int step;

		public UnitAndStep(int aUnit, int aStep) {
			super();
			unit = aUnit;
			step = aStep;
		}

		@Override
		public String toString() {
			return "UnitAndStep [step=" + step + ", unit=" + unit + "]";
		}
	}

	private void addRepeating(List<Deadline> tempDeadlines, Date tempDate, String tempInfo) {
		Calendar tempStartingPoint = Calendar.getInstance();
		UnitAndStep tempStepAndUnit = getStepAndUnit(tempInfo);
		LOGGER.info("stepAndUnit=" + tempStepAndUnit);
		int tempMaxAddCount;
		if (tempStepAndUnit.unit == Calendar.YEAR) {
			tempStartingPoint.add(Calendar.YEAR, -1);
			tempMaxAddCount = Math.max(1, 4 / tempStepAndUnit.step);
		} else if (tempStepAndUnit.unit == Calendar.MONTH) {
			tempStartingPoint.add(Calendar.MONTH, -3);
			tempMaxAddCount = Math.max(3, 12 / tempStepAndUnit.step);
		} else if (tempStepAndUnit.unit == Calendar.WEEK_OF_YEAR) {
			tempStartingPoint.add(Calendar.WEEK_OF_YEAR, -3);
			tempMaxAddCount = Math.max(10, 40 / tempStepAndUnit.step);
		} else {
			LOGGER.error("No valid range " + tempInfo);
			return;
		}
		Calendar tempCal = Calendar.getInstance();
		tempCal.set(Calendar.HOUR_OF_DAY, 0);
		tempCal.set(Calendar.MINUTE, 0);
		tempCal.set(Calendar.SECOND, 0);
		tempCal.set(Calendar.MILLISECOND, 0);
		int tempPreviousYear = tempCal.get(Calendar.YEAR) - 1;
		tempCal.setTime(tempDate);
		// Begin one year before
		int tempDateYear = tempCal.get(Calendar.YEAR);
		while (tempDateYear < tempPreviousYear) {
			tempCal.add(tempStepAndUnit.unit, tempStepAndUnit.step);
			tempDateYear = tempCal.get(Calendar.YEAR);
		}
		// LOGGER.info("tempCal="+tempCal.getTime());
		LOGGER.info("tempStartingPoint=" + tempStartingPoint.getTime());
		int tempAddCount = 0;
		while (true) {
			if (tempStartingPoint.before(tempCal)) {
				LOGGER.info("Match " + tempCal.getTime() + " for " + tempDate + " " + tempInfo);
				Deadline tempDeadline = new Deadline();
				Date tempWhen = tempCal.getTime();
				tempWhen = addTime(tempWhen, tempInfo);
				tempDeadline.setWhen(tempWhen);
				tempDeadline.setInfo(tempInfo);
				tempDeadline.setRepeating(tempDate);
				tempDeadlines.add(tempDeadline);
				tempAddCount++;
				if (tempAddCount >= tempMaxAddCount) {
					break;
				}
			}
			tempCal.add(tempStepAndUnit.unit, tempStepAndUnit.step);
		}
	}

	private UnitAndStep getStepAndUnit(String anInfo) {
		int tempSpace = anInfo.indexOf(" ");
		if (tempSpace > 0) {
			char tempType;
			Integer tempCount;
			if (tempSpace == 1) {
				// Annual event
				tempType = 'Y';
				tempCount = 1;
			} else {
				String tempNextWord = anInfo.substring(1, tempSpace);
				try {
					String tempCountString = tempNextWord.substring(0, tempNextWord.length() - 1);
					try {
						tempCount = new Integer(tempCountString);
						if (tempCount < 1) {
							tempCount = 1;
						}
						tempType = tempNextWord.charAt(tempNextWord.length() - 1);
					} catch (NumberFormatException e) {
						tempCount = 1;
						if (tempNextWord.length() == 1 || (tempNextWord.length() > 1 && tempNextWord.charAt(1) == ' ')) {
							tempType = tempNextWord.charAt(0);
						} else {
							// Annual event
							tempType = 'Y';
						}
					}
				} catch (Exception e) {
					LOGGER.info("Ignore " + anInfo);
					LOGGER.warn("Ignore", e);
					// Annual event
					tempType = 'Y';
					tempCount = 1;
				}
			}
			switch (tempType) {
			case 'w':
				return new UnitAndStep(Calendar.WEEK_OF_YEAR, tempCount);
			case 'W':
				return new UnitAndStep(Calendar.WEEK_OF_YEAR, tempCount);
			case 'm':
				return new UnitAndStep(Calendar.MONTH, tempCount);
			case 'M':
				return new UnitAndStep(Calendar.MONTH, tempCount);
			case 'y':
				return new UnitAndStep(Calendar.YEAR, tempCount);
			case 'Y':
				return new UnitAndStep(Calendar.YEAR, tempCount);
			default:
				break;
			}
		}
		return new UnitAndStep(Calendar.YEAR, 1);
	}

	@Override
	public void saveConfirmedTasks(List<Deadline> aDeadlines) {
		try {
			List<Deadline> tempDones = new ArrayList<Deadline>();
			for (Deadline tempDeadline : aDeadlines) {
				if (tempDeadline.isDone()) {
					tempDones.add(tempDeadline);
				}
			}
			if (tempDones.size() > 0) {
				FileWriter tempFileWriter = new FileWriter(getDirectory() + "/" + TERMIN_DONE_TXT, true);
				PrintWriter tempDone = new PrintWriter(new BufferedWriter(tempFileWriter));
				tempDone.println(INFO_PREFIX + new Date());
				for (Deadline tempDeadline : tempDones) {
					String tempLine = dateFormat.format(tempDeadline.getWhen()) + tempDeadline.getInfo();
					tempDone.println(tempLine);
					LOGGER.info(new Date() + ": Confirmed: '" + tempLine + "'");
				}
				tempDone.close();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}