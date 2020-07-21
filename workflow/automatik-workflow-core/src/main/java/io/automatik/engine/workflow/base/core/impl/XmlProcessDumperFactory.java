
package io.automatik.engine.workflow.base.core.impl;

public class XmlProcessDumperFactory {

	public static XmlProcessDumper newXmlProcessDumperFactory() {
		return getXmlProcessDumperFactoryService().newXmlProcessDumper();
	}

	public static XmlProcessDumperFactoryService getXmlProcessDumperFactoryService() {
		return null;
	}

	private XmlProcessDumperFactory() {
		// It is not allowed to create instances of util classes.
	}
}
