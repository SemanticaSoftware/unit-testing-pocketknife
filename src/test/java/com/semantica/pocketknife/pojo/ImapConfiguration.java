package com.semantica.pocketknife.pojo;

import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class ImapConfiguration {

	private List<String> folders;
	private String errorFolder;
	private ImapConnectionConfiguration connection;
	private boolean deleteIfNoError;

	public ImapConfiguration() {
		super();
	}

	public ImapConfiguration(List<String> folders, String errorFolder, ImapConnectionConfiguration connection,
			boolean deleteIfNoError) {
		super();
		this.folders = folders;
		this.errorFolder = errorFolder;
		this.connection = connection;
		this.deleteIfNoError = deleteIfNoError;
	}

	public List<String> getFolders() {
		return folders;
	}

	public void setFolders(List<String> folders) {
		this.folders = folders;
	}

	public String getErrorFolder() {
		return errorFolder;
	}

	public void setErrorFolder(String errorFolder) {
		this.errorFolder = errorFolder;
	}

	public ImapConnectionConfiguration getConnection() {
		return connection;
	}

	public void setConnection(ImapConnectionConfiguration connection) {
		this.connection = connection;
	}

	public boolean isDeleteIfNoError() {
		return deleteIfNoError;
	}

	public void setDeleteIfNoError(boolean deleteIfNoError) {
		this.deleteIfNoError = deleteIfNoError;
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
