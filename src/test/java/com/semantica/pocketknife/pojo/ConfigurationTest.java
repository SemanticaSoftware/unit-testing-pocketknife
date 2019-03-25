package com.semantica.pocketknife.pojo;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.semantica.pocketknife.pojo.ReflectionPojoTester;
import com.semantica.pocketknife.pojo.example.Configuration;
import com.semantica.pocketknife.pojo.example.ConnectorsConfiguration;
import com.semantica.pocketknife.pojo.example.ImapConfiguration;
import com.semantica.pocketknife.pojo.example.ImapConnectionConfiguration;
import com.semantica.pocketknife.pojo.example.MailArchiverConfiguration;
import com.semantica.pocketknife.pojo.example.MessageQueueConfiguration;
import com.semantica.pocketknife.pojo.example.RawMailDirConfiguration;

public class ConfigurationTest {

	@Test
	public void configurationPojoClassesShouldValidate() throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchFieldException, SecurityException {
		ReflectionPojoTester.testClassListForGettersSettersAndConstructors(Arrays.asList(Configuration.class,
				ConnectorsConfiguration.class, ImapConfiguration.class, ImapConnectionConfiguration.class,
				MailArchiverConfiguration.class, MessageQueueConfiguration.class, RawMailDirConfiguration.class));
	}

}
