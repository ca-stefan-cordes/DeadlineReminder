package de.quaddy_services.deadlinereminder.file;

import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.quaddy_services.deadlinereminder.Deadline;
import junit.framework.TestCase;

public class FileStorageTest extends TestCase {
	private static final DecimalFormat TWO = new DecimalFormat("00");

	private static final Logger LOGGER = LoggerFactory.getLogger(FileStorageTest.class);

	class TestFileStorage extends FileStorage {
		private String termin;

		public TestFileStorage(String aText) {
			termin = aText;
			LOGGER.info("termin=" + aText);
		}

		@Override
		protected Reader createReader(File aTempFile) {
			if (aTempFile.getName().equals(TERMIN_DONE_TXT)) {
				return new StringReader("");
			}
			return new StringReader(termin);
		}
	}

	public void testBirthday() {
		Calendar tempCal = Calendar.getInstance();
		tempCal.set(Calendar.YEAR, 1970);
		String tempText = TWO.format(tempCal.get(Calendar.DAY_OF_MONTH)) + "."
				+ TWO.format(tempCal.get(Calendar.MONTH) + 1) + "." + TWO.format(tempCal.get(Calendar.YEAR))
				+ "*Max Mustermann";
		TestFileStorage tempTestFileStorage = new TestFileStorage(tempText);
		Calendar tempTo = Calendar.getInstance();
		tempTo.add(Calendar.DAY_OF_YEAR, 400);
		List<Deadline> tempDeadlines = tempTestFileStorage.getOpenDeadlines(tempTo.getTime());
		LOGGER.info(tempDeadlines.toString());
		assertEquals(2, tempDeadlines.size());
		assertEquals("*Max Mustermann", tempDeadlines.get(0).getInfo());
	}

	public void test6Month() {
		Calendar tempCal = Calendar.getInstance();
		String tempText = TWO.format(tempCal.get(Calendar.DAY_OF_MONTH)) + "."
				+ TWO.format(tempCal.get(Calendar.MONTH) + 1) + "." + TWO.format(tempCal.get(Calendar.YEAR))
				+ "*6m Stefan";
		TestFileStorage tempTestFileStorage = new TestFileStorage(tempText);
		Calendar tempTo = Calendar.getInstance();
		tempTo.add(Calendar.DAY_OF_YEAR, 400);
		List<Deadline> tempDeadlines = tempTestFileStorage.getOpenDeadlines(tempTo.getTime());
		LOGGER.info(tempDeadlines.toString());
		assertEquals(3, tempDeadlines.size());
	}

	public void testEveryWeek() {
		Calendar tempCal = Calendar.getInstance();
		String tempText = TWO.format(tempCal.get(Calendar.DAY_OF_MONTH)) + "."
				+ TWO.format(tempCal.get(Calendar.MONTH) + 1) + "." + TWO.format(tempCal.get(Calendar.YEAR))
				+ "*w Week";
		TestFileStorage tempTestFileStorage = new TestFileStorage(tempText);
		Calendar tempTo = Calendar.getInstance();
		tempTo.add(Calendar.DAY_OF_YEAR, 40);
		List<Deadline> tempDeadlines = tempTestFileStorage.getOpenDeadlines(tempTo.getTime());
		LOGGER.info(tempDeadlines.toString());
		assertEquals(6, tempDeadlines.size());
	}

	public void testEvery1Week() {
		Calendar tempCal = Calendar.getInstance();
		String tempText = TWO.format(tempCal.get(Calendar.DAY_OF_MONTH)) + "."
				+ TWO.format(tempCal.get(Calendar.MONTH) + 1) + "." + TWO.format(tempCal.get(Calendar.YEAR))
				+ "*1w Week";
		TestFileStorage tempTestFileStorage = new TestFileStorage(tempText);
		Calendar tempTo = Calendar.getInstance();
		tempTo.add(Calendar.DAY_OF_YEAR, 40);
		List<Deadline> tempDeadlines = tempTestFileStorage.getOpenDeadlines(tempTo.getTime());
		LOGGER.info(tempDeadlines.toString());
		assertEquals(6, tempDeadlines.size());
	}

	public void testPastWeek() {
		TestFileStorage tempTestFileStorage = new TestFileStorage("23.02.2015*1w 16:00 Lisa Reitstunde");
		Calendar tempTo = Calendar.getInstance();
		tempTo.add(Calendar.DAY_OF_YEAR, 42);
		List<Deadline> tempDeadlines = tempTestFileStorage.getOpenDeadlines(tempTo.getTime());
		LOGGER.info(tempDeadlines.toString());
		if (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
			// on monday one week less is returned as 23.02.2015 is monday.
			assertEquals(8, tempDeadlines.size());
		} else {
			assertEquals(9, tempDeadlines.size());
		}

	}

	public void testDaily() {
		Calendar tempCal = Calendar.getInstance();
		String tempText = format(tempCal) + "*1d testDaily";
		TestFileStorage tempTestFileStorage = new TestFileStorage(tempText);
		Calendar tempTo = Calendar.getInstance();
		tempTo.add(Calendar.DAY_OF_YEAR, 40);
		List<Deadline> tempDeadlines = tempTestFileStorage.getOpenDeadlines(tempTo.getTime());
		LOGGER.info(tempDeadlines.toString());
		assertEquals(30, tempDeadlines.size());
		assertEquals("*testDaily", tempDeadlines.get(0).getTextWithoutRepeatingInfo());
		assertEquals("*1d testDaily", tempDeadlines.get(0).getInfo());
	}

	public void testDailyWithTime() {
		Calendar tempCal = Calendar.getInstance();
		String tempText = format(tempCal) + "*1d 11:00 testDaily";
		TestFileStorage tempTestFileStorage = new TestFileStorage(tempText);
		Calendar tempTo = Calendar.getInstance();
		tempTo.add(Calendar.DAY_OF_YEAR, 40);
		List<Deadline> tempDeadlines = tempTestFileStorage.getOpenDeadlines(tempTo.getTime());
		LOGGER.info(tempDeadlines.toString());
		assertEquals(30, tempDeadlines.size());
		assertEquals("*11:00 testDaily", tempDeadlines.get(0).getTextWithoutRepeatingInfo());
		assertEquals("*1d 11:00 testDaily", tempDeadlines.get(0).getInfo());
	}

	public void testElevenDays() {
		Calendar tempCal = Calendar.getInstance();
		String tempText = format(tempCal) + "*11d testDaily";
		TestFileStorage tempTestFileStorage = new TestFileStorage(tempText);
		Calendar tempTo = Calendar.getInstance();
		tempTo.add(Calendar.DAY_OF_YEAR, 40);
		List<Deadline> tempDeadlines = tempTestFileStorage.getOpenDeadlines(tempTo.getTime());
		LOGGER.info(tempDeadlines.toString());
		assertEquals(4, tempDeadlines.size());
		assertEquals("*testDaily", tempDeadlines.get(0).getTextWithoutRepeatingInfo());
		assertEquals("*11d testDaily", tempDeadlines.get(0).getInfo());
	}

	public void testDailyWithEnd() {
		Calendar tempCal = Calendar.getInstance();
		String tempToDay = format(tempCal);
		tempCal.add(Calendar.DAY_OF_YEAR, 10);
		String tempTenDays = format(tempCal);
		String tempText = tempToDay + "*1d" + "-" + tempTenDays + " testDailyWithEnd";
		TestFileStorage tempTestFileStorage = new TestFileStorage(tempText);
		Calendar tempTo = Calendar.getInstance();
		tempTo.add(Calendar.DAY_OF_YEAR, 40);
		List<Deadline> tempDeadlines = tempTestFileStorage.getOpenDeadlines(tempTo.getTime());
		LOGGER.info(tempDeadlines.toString());
		assertEquals(11, tempDeadlines.size());
		assertEquals("*testDailyWithEnd", tempDeadlines.get(0).getTextWithoutRepeatingInfo());
		assertEquals("*1d-25.05.2018 testDailyWithEnd", tempDeadlines.get(0).getInfo());
	}

	public void testDailyWithTimeAndWithEnd() {
		Calendar tempCal = Calendar.getInstance();
		String tempToDay = format(tempCal);
		tempCal.add(Calendar.DAY_OF_YEAR, 10);
		String tempTenDays = format(tempCal);
		String tempText = tempToDay + "*1d" + "-" + tempTenDays + " 11:00 testDailyWithEnd";
		TestFileStorage tempTestFileStorage = new TestFileStorage(tempText);
		Calendar tempTo = Calendar.getInstance();
		tempTo.add(Calendar.DAY_OF_YEAR, 40);
		List<Deadline> tempDeadlines = tempTestFileStorage.getOpenDeadlines(tempTo.getTime());
		LOGGER.info(tempDeadlines.toString());
		assertEquals(11, tempDeadlines.size());
		assertEquals("*11:00 testDailyWithEnd", tempDeadlines.get(0).getTextWithoutRepeatingInfo());
		assertEquals("*1d-" + tempTenDays + " 11:00 testDailyWithEnd", tempDeadlines.get(0).getInfo());
	}

	/**
	 *
	 */
	private String format(Calendar tempCal) {
		String tempToDay = TWO.format(tempCal.get(Calendar.DAY_OF_MONTH)) + "."
				+ TWO.format(tempCal.get(Calendar.MONTH) + 1) + "." + TWO.format(tempCal.get(Calendar.YEAR));
		return tempToDay;
	}

}
