package com.semantica.pocketknife.util;

public class TestUtils {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TestUtils.class);

	public static void traceLogMethodCall() {
		log.trace("In method: " + Thread.currentThread().getStackTrace()[3] + ", called from: "
				+ Thread.currentThread().getStackTrace()[4]);
	}

	public static String getCount(int number) {
		if (number % 10 == 1 && number != 11) {
			return number + "st";
		} else if (number % 10 == 2 && number != 12) {
			return number + "nd";
		} else if (number % 10 == 3 && number != 13) {
			return number + "rd";
		} else {
			return number + "th";
		}
	}

}
