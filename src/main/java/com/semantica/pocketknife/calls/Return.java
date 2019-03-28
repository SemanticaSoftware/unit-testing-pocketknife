package com.semantica.pocketknife.calls;

/**
 * Class supporting the fluent expression of the amount of times a mock should
 * return a value.
 *
 * @author A. Haanstra
 *
 */
public class Return {

	public static final Return NEVER = Return.times(0);
	public static final Return ONCE = Return.times(1);
	public static final Return TWICE = Return.times(2);
	public static final Return THRICE = Return.times(3);

	public final int times;

	protected Return(int times) {
		super();
		this.times = times;
	}

	public static Return times(int times) {
		return new Return(times);
	}

	public int getTimes() {
		return times;
	}
}