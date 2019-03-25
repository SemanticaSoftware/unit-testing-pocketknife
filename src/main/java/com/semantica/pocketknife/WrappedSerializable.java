package com.semantica.pocketknife;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * This class functions to wrap test data object and provide an easy way to
 * check and compare their json and yaml contents. Also it overrides toString,
 * returning the object serialized to the default type. This is particulary
 * useful when the the wrapper object is referenced as Object.
 *
 * @author A. Haanstra
 *
 * @param <S>
 */
public class WrappedSerializable<S extends Serializable> {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WrappedSerializable.class);

	public enum SerializationType {
		JSON, YAML;
	}

	private S serializable;
	private static ObjectMapper objectToJsonMapper = new ObjectMapper();
	private static ObjectMapper objectToYamlMapper = new ObjectMapper(new YAMLFactory());
	private SerializationType defaultSerializationType;

	/**
	 * Constructs a WrappedSerializable object around a Serializable object and sets
	 * the default serialization type used by {@link #toString()}.
	 *
	 * @param serializable             The wrapped object used in the unit test
	 * @param defaultSerializationType The default serialization type used by
	 *                                 {@link #toString()}
	 */
	public WrappedSerializable(S serializable, SerializationType defaultSerializationType) {
		super();
		this.serializable = serializable;
		this.defaultSerializationType = defaultSerializationType;
	}

	/**
	 *
	 * @return The wrapped object
	 */
	public S getObject() {
		return serializable;
	}

	/**
	 *
	 * @return The JSON representation of the wrapped object
	 * @throws JsonProcessingException
	 */
	public String getJson() throws JsonProcessingException {
		return objectToJsonMapper.writeValueAsString(serializable);
	}

	/**
	 *
	 * @return The YAML representation of the wrapped object
	 * @throws JsonProcessingException
	 */
	public String getYaml() throws JsonProcessingException {
		return objectToYamlMapper.writeValueAsString(serializable);
	}

	/**
	 * Serializes the wrapped serializable object to the default serialization type.
	 *
	 */
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

	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}

	@Override
	public boolean equals(Object other) {
		return EqualsBuilder.reflectionEquals(this, other);
	}
}
