package com.semantica.pocketknife.pojo;

public enum Protocol {
	IMAP("imap"), IMAPS("imaps");

	private String protocol;

	private Protocol(String protocol) {
		this.protocol = protocol;
	}

	public String getProtocolString() {
		return protocol;
	}
}
