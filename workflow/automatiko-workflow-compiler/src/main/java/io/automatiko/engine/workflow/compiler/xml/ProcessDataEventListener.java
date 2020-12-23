
package io.automatiko.engine.workflow.compiler.xml;

import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.workflow.process.core.Node;

public interface ProcessDataEventListener {

	void onNodeAdded(Node node);

	void onProcessAdded(Process process);

	void onMetaDataAdded(String name, Object data);

	void onComplete(Process process);

	void onBuildComplete(Process process);
}
