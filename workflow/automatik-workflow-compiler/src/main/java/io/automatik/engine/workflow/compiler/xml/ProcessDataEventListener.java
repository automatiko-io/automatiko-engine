
package io.automatik.engine.workflow.compiler.xml;

import io.automatik.engine.api.definition.process.Process;
import io.automatik.engine.workflow.process.core.Node;

public interface ProcessDataEventListener {

	void onNodeAdded(Node node);

	void onProcessAdded(Process process);

	void onMetaDataAdded(String name, Object data);

	void onComplete(Process process);

	void onBuildComplete(Process process);
}
