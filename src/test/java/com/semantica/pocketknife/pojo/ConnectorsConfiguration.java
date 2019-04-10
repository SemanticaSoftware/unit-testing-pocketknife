package com.semantica.pocketknife.pojo;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class ConnectorsConfiguration {

	private MailArchiverConfiguration mailArchiver;
	private RawMailDirConfiguration rawMailDir;
	private MessageQueueConfiguration messageQueue;

	public ConnectorsConfiguration() {
		super();
	}

	public ConnectorsConfiguration(MailArchiverConfiguration mailArchiver, RawMailDirConfiguration rawMailDir,
			MessageQueueConfiguration messageQueue) {
		super();
		this.mailArchiver = mailArchiver;
		this.rawMailDir = rawMailDir;
		this.messageQueue = messageQueue;
	}

	public MailArchiverConfiguration getMailArchiver() {
		return mailArchiver;
	}

	public void setMailArchiver(MailArchiverConfiguration mailArchiver) {
		this.mailArchiver = mailArchiver;
	}

	public RawMailDirConfiguration getRawMailDir() {
		return rawMailDir;
	}

	public void setRawMailDir(RawMailDirConfiguration rawMailDir) {
		this.rawMailDir = rawMailDir;
	}

	public MessageQueueConfiguration getMessageQueue() {
		return messageQueue;
	}

	public void setMessageQueue(MessageQueueConfiguration messageQueue) {
		this.messageQueue = messageQueue;
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
