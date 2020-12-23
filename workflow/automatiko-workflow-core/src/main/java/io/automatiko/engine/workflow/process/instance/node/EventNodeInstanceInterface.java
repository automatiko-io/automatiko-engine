
package io.automatiko.engine.workflow.process.instance.node;

public interface EventNodeInstanceInterface {

	void signalEvent(String type, Object event);

}
