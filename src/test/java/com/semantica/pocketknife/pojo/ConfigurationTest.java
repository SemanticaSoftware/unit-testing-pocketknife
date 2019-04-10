package com.semantica.pocketknife.pojo;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.semantica.pocketknife.pojo.ReflectionPojoTester;

public class ConfigurationTest {

	@Test
	public void configurationPojoClassesShouldValidate() throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchFieldException, SecurityException {
		ReflectionPojoTester.testClassListForGettersSettersAndConstructors(Arrays.asList(Configuration.class,
				ConnectorsConfiguration.class, ImapConfiguration.class, ImapConnectionConfiguration.class,
				MailArchiverConfiguration.class, MessageQueueConfiguration.class, RawMailDirConfiguration.class));
	}

}
