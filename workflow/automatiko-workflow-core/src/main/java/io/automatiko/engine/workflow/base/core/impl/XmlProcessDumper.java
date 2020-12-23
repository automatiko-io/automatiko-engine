
package io.automatiko.engine.workflow.base.core.impl;

import io.automatiko.engine.api.definition.process.Process;

public interface XmlProcessDumper {

	String dumpProcess(io.automatiko.engine.api.definition.process.Process process);

	Process readProcess(String processXml);

}
