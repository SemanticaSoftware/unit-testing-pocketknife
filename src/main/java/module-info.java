/**
 *
 */
/**
 * @author auke
 *
 */
module com.semantica.pocketknife {
	exports com.semantica.pocketknife.mock;
	exports com.semantica.pocketknife;
	exports com.semantica.pocketknife.calls;
	exports com.semantica.pocketknife.methodrecorder;
	exports com.semantica.pocketknife.methodrecorder.dynamicproxies;
	exports com.semantica.pocketknife.pojo;
	exports com.semantica.pocketknife.util;
	exports com.semantica.pocketknife.mock.dto;
	exports com.semantica.pocketknife.mock.service;
	exports com.semantica.pocketknife.mock.service.support.components;
	exports com.semantica.pocketknife.mock.service.support;

	opens com.semantica.pocketknife.calls to org.apache.commons.lang3;
	opens com.semantica.pocketknife to com.fasterxml.jackson.databind;
	opens com.semantica.pocketknife.pojo to org.apache.commons.lang3;
	opens com.semantica.pocketknife.pojo.example to org.apache.commons.lang3;

	requires com.fasterxml.jackson.core;
	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.dataformat.yaml;
	requires javax.inject;
	requires org.apache.commons.lang3;
	requires org.hamcrest;
	requires org.objenesis;
	requires org.opentest4j;
	requires slf4j.api;
	requires net.bytebuddy;
	requires jdk.unsupported;

}