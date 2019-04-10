package com.semantica.pocketknife;

import java.io.Serializable;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.semantica.pocketknife.WrappedSerializable.SerializationType;

public class WrappedSerializableTest {

	private class SomeSerializable implements Serializable {
		private static final long serialVersionUID = 1L;
		private String myValue = "test";

		public String getMyValue() {
			return myValue;
		}

		public void setMyValue(String myValue) {
			this.myValue = myValue;
		}

	}

	@Test
	public void toStringShouldSerializeToJson() throws JsonProcessingException {
		WrappedSerializable<SomeSerializable> test = new WrappedSerializable<>(new SomeSerializable(),
				SerializationType.JSON);
		String expectedJson = "{\"myValue\":\"test\"}";
		assert test.getJson().equals(expectedJson);
		assert test.toString().equals(expectedJson);
	}

	@Test
	public void toStringShouldSerializeToYAML() throws JsonProcessingException {
		WrappedSerializable<SomeSerializable> test = new WrappedSerializable<>(new SomeSerializable(),
				SerializationType.YAML);
		String expectedYaml = "---\n" + "myValue: \"test\"\n";
		Assertions.assertEquals(expectedYaml, test.getYaml());
		Assertions.assertEquals(expectedYaml, test.toString());
	}

	@Test
	public void inputAndOutputObjectShouldBeTheSame() {
		SomeSerializable serializable = new SomeSerializable();
		WrappedSerializable<SomeSerializable> test = new WrappedSerializable<>(serializable, SerializationType.YAML);
		assert test.getObject() == serializable;
	}

	@Test
	public void shouldThroxwException() throws JsonProcessingException {
		WrappedSerializable<Serializable> test = new WrappedSerializable<>(new Serializable() {
			private static final long serialVersionUID = 1L;
		}, SerializationType.YAML);
		Assertions.assertThrows(IllegalStateException.class, test::toString);
	}

}
