package com.semantica.pocketknife.pojo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

class GetterSetterPair {
	public final Method getter;
	public final Method setter;
	public final Field correspondingField;

	public GetterSetterPair(Method getter, Method setter, Field correspondingField) {
		this.getter = getter;
		this.setter = setter;
		if (correspondingField != null) {
			correspondingField.setAccessible(true);
		}
		this.correspondingField = correspondingField;
	}
}
