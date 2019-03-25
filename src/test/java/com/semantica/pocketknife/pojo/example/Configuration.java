package com.semantica.pocketknife.pojo.example;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/* TODO: This example pojo structure has been quickly included to test some of the features of the
 * ReflectionPojoTester. This needs to be replaced in the future by multiple POJOs to test exactly
 * all different points in the ReflectionPojoTester code.
 */
public class Configuration implements Serializable {

	private static final long serialVersionUID = 1L;

	private ImapConfiguration imap;
	private ConnectorsConfiguration connectors;

	public Configuration() {
		super();
	}

	public Configuration(ImapConfiguration imap, ConnectorsConfiguration connectors) {
		super();
		this.imap = imap;
		this.connectors = connectors;
	}

	public ImapConfiguration getImap() {
		return imap;
	}

	public void setImap(ImapConfiguration imap) {
		this.imap = imap;
	}

	public ConnectorsConfiguration getConnectors() {
		return connectors;
	}

	public void setConnectors(ConnectorsConfiguration connectors) {
		this.connectors = connectors;
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
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
