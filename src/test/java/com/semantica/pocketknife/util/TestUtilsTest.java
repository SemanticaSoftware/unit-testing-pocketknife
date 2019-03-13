package com.semantica.pocketknife.util;

import org.junit.jupiter.api.Test;

public class TestUtilsTest {

	@Test
	public void shouldAppendCorrectEnglishCountPostfix() {
		assert TestUtils.getCount(0).equals("0th");
		assert TestUtils.getCount(1).equals("1st");
		assert TestUtils.getCount(2).equals("2nd");
		assert TestUtils.getCount(3).equals("3rd");
		assert TestUtils.getCount(4).equals("4th");
		assert TestUtils.getCount(10).equals("10th");
		assert TestUtils.getCount(11).equals("11th");
		assert TestUtils.getCount(12).equals("12th");
		assert TestUtils.getCount(13).equals("13th");
		assert TestUtils.getCount(14).equals("14th");
		assert TestUtils.getCount(20).equals("20th");
		assert TestUtils.getCount(21).equals("21st");
		assert TestUtils.getCount(22).equals("22nd");
		assert TestUtils.getCount(23).equals("23rd");
		assert TestUtils.getCount(24).equals("24th");
	}

}
