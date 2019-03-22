package com.semantica.pocketknife.util;

import java.util.Arrays;

public class MathUtil {

	public static long[] widenToLongArray(byte[] bytes) {
		int length = bytes.length;
		long[] numbers = new long[length];
		for (int i = 0; i < length; i++) {
			numbers[i] = bytes[i];
		}
		return numbers;
	}

	public static long[] widenToLongArray(char[] chars) {
		int length = chars.length;
		long[] numbers = new long[length];
		for (int i = 0; i < length; i++) {
			numbers[i] = chars[i];
		}
		return numbers;
	}

	public static long[] widenToLongArray(short[] shorts) {
		int length = shorts.length;
		long[] numbers = new long[length];
		for (int i = 0; i < length; i++) {
			numbers[i] = shorts[i];
		}
		return numbers;
	}

	public static double[] widenToDoubleArray(byte[] bytes) {
		int length = bytes.length;
		double[] numbers = new double[length];
		for (int i = 0; i < length; i++) {
			numbers[i] = bytes[i];
		}
		return numbers;
	}

	public static double[] widenToDoubleArray(char[] chars) {
		int length = chars.length;
		double[] numbers = new double[length];
		for (int i = 0; i < length; i++) {
			numbers[i] = chars[i];
		}
		return numbers;
	}

	public static double[] widenToDoubleArray(short[] shorts) {
		int length = shorts.length;
		double[] numbers = new double[length];
		for (int i = 0; i < length; i++) {
			numbers[i] = shorts[i];
		}
		return numbers;
	}

	public static double standardDeviation(byte[] bytes) {
		double[] numbers = widenToDoubleArray(bytes);
		return standardDeviation(numbers);
	}

	public static double standardDeviation(char[] chars) {
		double[] numbers = widenToDoubleArray(chars);
		return standardDeviation(numbers);
	}

	public static double standardDeviation(short[] shorts) {
		double[] numbers = widenToDoubleArray(shorts);
		return standardDeviation(numbers);
	}

	public static double standardDeviation(double[] numbers) {
		int n = numbers.length;
		double sum = Arrays.stream(numbers).sum();
		double mean = sum / n;
		double sumOfSquaredDifferencesFromMean = Arrays.stream(numbers).map(x -> Math.pow(x - mean, 2)).sum();
		return Math.sqrt(sumOfSquaredDifferencesFromMean / n);
	}

	public static double expectedStandardDeviation(double min, double max, int precisionPoints) {
		assert max > min;
		double width = max - min;
		double step = width / (precisionPoints - 1);
		double[] points = new double[precisionPoints];
		for (int i = 0; i < precisionPoints; i++) {
			points[i] = min + step * i;
		}
		return standardDeviation(points);
	}

}
