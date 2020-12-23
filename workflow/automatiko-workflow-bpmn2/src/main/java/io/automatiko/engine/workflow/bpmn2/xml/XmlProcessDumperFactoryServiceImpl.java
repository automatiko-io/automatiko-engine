
package io.automatiko.engine.workflow.bpmn2.xml;

import io.automatiko.engine.workflow.base.core.impl.XmlProcessDumper;
import io.automatiko.engine.workflow.base.core.impl.XmlProcessDumperFactoryService;

public class XmlProcessDumperFactoryServiceImpl implements XmlProcessDumperFactoryService {

	public XmlProcessDumper newXmlProcessDumper() {
		return XmlBPMNProcessDumper.INSTANCE;
	}

}
