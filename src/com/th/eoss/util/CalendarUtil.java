package com.th.eoss.util;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class CalendarUtil {
		
	public static String asOfWeek() {
		Calendar c = Calendar.getInstance(Locale.US);
		c.setTimeZone(TimeZone.getTimeZone("Asia/Bangkok"));
		c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		int monday = c.get(Calendar.DAY_OF_MONTH);
		c.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
		int friday = c.get(Calendar.DAY_OF_MONTH);
		int month = c.get(Calendar.MONTH) + 1;
		int year = c.get(Calendar.YEAR);
		return String.format("%d-%d/%d/%d", monday,friday,month,year);
	}

}
