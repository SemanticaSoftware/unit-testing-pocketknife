package com.semantica.pocketknife.pojo;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class MailArchiverConfiguration {

	private String host;
	private int port;
	private long connectionRetryTimeoutMinutes;
	private String mailArchiverConnectorConfigurationStub = "MailArchiverConnectorConfigurationStub.xml";
	private boolean enabled;

	public MailArchiverConfiguration() {
		super();
	}

	public MailArchiverConfiguration(String host, int port, long connectionRetryTimeoutMinutes,
			String mailArchiverConnectorConfigurationStub, boolean enabled) {
		super();
		this.host = host;
		this.port = port;
		this.connectionRetryTimeoutMinutes = connectionRetryTimeoutMinutes;
		this.mailArchiverConnectorConfigurationStub = mailArchiverConnectorConfigurationStub;
		this.enabled = enabled;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public long getConnectionRetryTimeoutMinutes() {
		return connectionRetryTimeoutMinutes;
	}

	public void setConnectionRetryTimeoutMinutes(long connectionRetryTimeoutMinutes) {
		this.connectionRetryTimeoutMinutes = connectionRetryTimeoutMinutes;
	}

	public String getMailArchiverConnectorConfigurationStub() {
		return mailArchiverConnectorConfigurationStub;
	}

	public void setMailArchiverConnectorConfigurationStub(String mailArchiverConnectorConfigurationStub) {
		this.mailArchiverConnectorConfigurationStub = mailArchiverConnectorConfigurationStub;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
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
