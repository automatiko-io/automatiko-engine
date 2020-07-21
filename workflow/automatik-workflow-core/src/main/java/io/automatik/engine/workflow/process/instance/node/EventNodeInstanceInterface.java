
package io.automatik.engine.workflow.process.instance.node;

public interface EventNodeInstanceInterface {

	void signalEvent(String type, Object event);

}
