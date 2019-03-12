package com.semantica.pocketknife;

import java.io.Serializable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class WrappedSerializable<S extends Serializable> {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WrappedSerializable.class);

	public enum SerializationType {
		JSON, YAML;
	}

	private S serializable;
	private static ObjectMapper objectToJsonMapper = new ObjectMapper();
	private static ObjectMapper objectToYamlMapper = new ObjectMapper(new YAMLFactory());
	private SerializationType defaultSerializationType;

	public WrappedSerializable(S serializable, SerializationType defaultSerializationType) {
		super();
		this.serializable = serializable;
		this.defaultSerializationType = defaultSerializationType;
	}

	public S getObject() {
		return serializable;
	}

	public String getJson() throws JsonProcessingException {
		return objectToJsonMapper.writeValueAsString(serializable);
	}

	public String getYaml() throws JsonProcessingException {
		return objectToYamlMapper.writeValueAsString(serializable);
	}

	@Override
	public String toString() {
		try {
			switch (defaultSerializationType) {
			case JSON:
				return getJson();
			case YAML:
				return getYaml();
			default:
				throw new IllegalStateException("Unknown default serialization type set.");
			}
		} catch (JsonProcessingException e) {
			log.error(
					"In method: {}, called from: {}, error: Problem encountered during serialization. Returning serializable.toString(): {}.",
					Thread.currentThread().getStackTrace()[1], Thread.currentThread().getStackTrace()[2],
					serializable.toString(), e);
			throw new IllegalStateException(e);
		}

	}

}
