package com.semantica.pocketknife.calls;

/**
 * Class supporting the fluent expression of the amount of times a method is
 * expected to have been invoked.
 *
 * @author A. Haanstra
 *
 */
public class Invoked {

	public static final Invoked NEVER = Invoked.times(0);
	public static final Invoked ONCE = Invoked.times(1);
	public static final Invoked TWICE = Invoked.times(2);
	public static final Invoked THRICE = Invoked.times(3);

	public final int times;

	protected Invoked(int times) {
		super();
		this.times = times;
	}

	public static Invoked times(int times) {
		return new Invoked(times);
	}

	public int getTimes() {
		return times;
	}
}