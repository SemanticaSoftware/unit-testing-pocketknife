package com.semantica.pocketknife;

import com.semantica.pocketknife.calls.Calls;

public interface Mock {

	public Calls<?> getCalls();

	public void reset();

}
