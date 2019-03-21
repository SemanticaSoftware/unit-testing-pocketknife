package com.semantica.pocketknife.util;

import org.junit.jupiter.api.Test;

public class TestUtilsTest {

	@Test
	public void shouldAppendCorrectEnglishOrdinalPostfix() {
		assert TestUtils.getOrdinal(0).equals("0th");
		assert TestUtils.getOrdinal(1).equals("1st");
		assert TestUtils.getOrdinal(2).equals("2nd");
		assert TestUtils.getOrdinal(3).equals("3rd");
		assert TestUtils.getOrdinal(4).equals("4th");
		assert TestUtils.getOrdinal(10).equals("10th");
		assert TestUtils.getOrdinal(11).equals("11th");
		assert TestUtils.getOrdinal(12).equals("12th");
		assert TestUtils.getOrdinal(13).equals("13th");
		assert TestUtils.getOrdinal(14).equals("14th");
		assert TestUtils.getOrdinal(20).equals("20th");
		assert TestUtils.getOrdinal(21).equals("21st");
		assert TestUtils.getOrdinal(22).equals("22nd");
		assert TestUtils.getOrdinal(23).equals("23rd");
		assert TestUtils.getOrdinal(24).equals("24th");
	}

	@Test
	public void shouldReturnCorrectMethodName() {
		TestUtils.traceLogMethodCall(0);
		assert TestUtils.getMethodName(0).equals("shouldReturnCorrectMethodName");
	}

}
