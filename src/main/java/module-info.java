/**
 *
 */
/**
 * @author auke
 *
 */
open module com.semantica.pocketknife {
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

//	opens com.semantica.pocketknife.calls to org.apache.commons.lang3, org.mockito;
//	opens com.semantica.pocketknife to com.fasterxml.jackson.databind, org.mockito;
//	opens com.semantica.pocketknife.pojo to org.apache.commons.lang3;
	
	//otherwise tests fail! - however, then this causes:
//	java --module-path ./unit-testing-pocketknife-0.0.2-SNAPSHOT.jar --list-modules
//	Error occurred during initialization of boot layer
//	java.lang.module.FindException: Error reading module: ./unit-testing-pocketknife-0.0.2-SNAPSHOT.jar
//	Caused by: java.lang.module.InvalidModuleDescriptorException: Package com.semantica.pocketknife.pojo.example not found in module
	// -->
//		opens com.semantica.pocketknife.pojo.example to org.apache.commons.lang3;
//	Strangely this prevents the following error:
//		[WARNING] [WORKER]   JUnit Jupiter:MocksRegistryTest:verifiedmocksShouldCauseStaticVerificationToSucceed()
//		[WARNING] [WORKER]     MethodSource [className = com.semantica.pocketknife.MocksRegistryTest, methodName = verifiedmocksShouldCauseStaticVerificationToSucceed, methodParameterTypes = ']
//		[WARNING] [WORKER]     => Wanted but not invoked:
//		[WARNING] [WORKER] calls.verifyNoMoreMethodInvocations(false);
//		[WARNING] [WORKER] -> at com.semantica.pocketknife/com.semantica.pocketknife.MocksRegistryTest.verifiedmocksShouldCauseStaticVerificationToSucceed(MocksRegistryTest.java:101)
//		[WARNING] [WORKER] Actually, there were zero interactions with this mock.
//  when enabling above line
	

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