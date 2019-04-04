package com.semantica.pocketknife.methodrecorder;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.semantica.pocketknife.util.Assert;
import com.semantica.pocketknife.util.MathUtil;

public class RandomIdentifierValuesTest {

	@Test
	public void shouldNotEqualForTwoInstances() {
		SomeClass someInstance = RandomIdentifierValues.identifierValue(SomeClass.class);
		SomeClass otherInstance = RandomIdentifierValues.identifierValue(SomeClass.class);
		assert !someInstance.equals(otherInstance);
	}

	@Test
	public void shouldNotEqualHashCodesForTwoInstances() {
		SomeClass someInstance = RandomIdentifierValues.identifierValue(SomeClass.class);
		SomeClass otherInstance = RandomIdentifierValues.identifierValue(SomeClass.class);
		assert someInstance.hashCode() != otherInstance.hashCode();
	}

	@Test
	public void shouldEqualCachedClassForTwoInstances() {
		SomeClass someInstance = RandomIdentifierValues.identifierValue(SomeClass.class);
		SomeClass otherInstance = RandomIdentifierValues.identifierValue(SomeClass.class);
		Assert.actual(someInstance.getClass().getName())
				.equalsExpected("com.semantica.pocketknife.methodrecorder.dynamicproxies.SomeClassIdentifyingProxy");
		assert someInstance.getClass().equals(otherInstance.getClass());
	}

	@Test
	public void shouldNotEqualForTwoArrayInstances() {
		int[] someInstance = RandomIdentifierValues.identifierValue(int[].class);
		int[] otherInstance = RandomIdentifierValues.identifierValue(int[].class);
		assert !someInstance.equals(otherInstance);
	}

	@Test
	public void shouldNotEqualHashCodesForTwoArrayInstances() {
		int[] someInstance = RandomIdentifierValues.identifierValue(int[].class);
		int[] otherInstance = RandomIdentifierValues.identifierValue(int[].class);
		assert someInstance.hashCode() != otherInstance.hashCode();
	}

	@Test
	public void byteShouldBeRandomizedAsExpected() {
		int length = 1000;
		Class<?> clazz = byte.class;
		byte[] randomChars = new byte[length];
		for (int i = 0; i < length; i++) {
			randomChars[i] = RandomIdentifierValues.identifierValue(byte.class);
		}
		System.out.println(clazz.getSimpleName() + ":\tσ_expected=\t"
				+ MathUtil.expectedStandardDeviation(Byte.MIN_VALUE, Byte.MAX_VALUE, 1000));
		System.out.println(clazz.getSimpleName() + ":\tσ_obs=\t\t" + MathUtil.standardDeviation(randomChars));
		System.out.println(clazz.getSimpleName() + ":\tx_min=\t\t" + (int) Byte.MIN_VALUE);
		System.out.println(clazz.getSimpleName() + ":\tx_min_obs=\t"
				+ Arrays.stream(MathUtil.widenToLongArray(randomChars)).min().getAsLong());
		System.out.println(clazz.getSimpleName() + ":\tx_max=\t\t" + (int) Byte.MAX_VALUE);
		System.out.println(clazz.getSimpleName() + ":\tx_max_obs=\t"
				+ Arrays.stream(MathUtil.widenToLongArray(randomChars)).max().getAsLong());
	}

	@Test
	public void charShouldBeRandomizedAsExpected() {
		int length = 1000;
		Class<?> clazz = char.class;
		char[] randomChars = new char[length];
		for (int i = 0; i < length; i++) {
			randomChars[i] = RandomIdentifierValues.identifierValue(char.class);
		}
		System.out.println(clazz.getSimpleName() + ":\tσ_expected=\t"
				+ MathUtil.expectedStandardDeviation(Character.MIN_VALUE, Character.MAX_VALUE, 1000));
		System.out.println(clazz.getSimpleName() + ":\tσ_obs=\t\t" + MathUtil.standardDeviation(randomChars));
		System.out.println(clazz.getSimpleName() + ":\tx_min=\t\t" + (int) Character.MIN_VALUE);
		System.out.println(clazz.getSimpleName() + ":\tx_min_obs=\t"
				+ Arrays.stream(MathUtil.widenToLongArray(randomChars)).min().getAsLong());
		System.out.println(clazz.getSimpleName() + ":\tx_max=\t\t" + (int) Character.MAX_VALUE);
		System.out.println(clazz.getSimpleName() + ":\tx_max_obs=\t"
				+ Arrays.stream(MathUtil.widenToLongArray(randomChars)).max().getAsLong());
	}

	@Test
	public void shortShouldBeRandomizedAsExpected() {
		int length = 1000;
		Class<?> clazz = short.class;
		short[] randomShorts = new short[length];
		for (int i = 0; i < length; i++) {
			randomShorts[i] = RandomIdentifierValues.identifierValue(short.class);
		}
		System.out.println(clazz.getSimpleName() + ":\tσ_expected=\t"
				+ MathUtil.expectedStandardDeviation(Short.MIN_VALUE, Short.MAX_VALUE, 1000));
		System.out.println(clazz.getSimpleName() + ":\tσ_obs=\t\t" + MathUtil.standardDeviation(randomShorts));
		System.out.println(clazz.getSimpleName() + ":\tx_min=\t\t" + (int) Short.MIN_VALUE);
		System.out.println(clazz.getSimpleName() + ":\tx_min_obs=\t"
				+ Arrays.stream(MathUtil.widenToLongArray(randomShorts)).min().getAsLong());
		System.out.println(clazz.getSimpleName() + ":\tx_max=\t\t" + (int) Short.MAX_VALUE);
		System.out.println(clazz.getSimpleName() + ":\tx_max_obs=\t"
				+ Arrays.stream(MathUtil.widenToLongArray(randomShorts)).max().getAsLong());
	}

}
