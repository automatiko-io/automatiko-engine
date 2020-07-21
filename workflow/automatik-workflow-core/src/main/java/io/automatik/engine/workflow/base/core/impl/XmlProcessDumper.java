
package io.automatik.engine.workflow.base.core.impl;

import io.automatik.engine.api.definition.process.Process;

public interface XmlProcessDumper {

	String dumpProcess(io.automatik.engine.api.definition.process.Process process);

	Process readProcess(String processXml);

}
