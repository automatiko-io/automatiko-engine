
package io.automatik.engine.workflow.bpmn2.xml;

import io.automatik.engine.workflow.base.core.impl.XmlProcessDumper;
import io.automatik.engine.workflow.base.core.impl.XmlProcessDumperFactoryService;

public class XmlProcessDumperFactoryServiceImpl implements XmlProcessDumperFactoryService {

	public XmlProcessDumper newXmlProcessDumper() {
		return XmlBPMNProcessDumper.INSTANCE;
	}

}
