
package io.automatiko.engine.workflow.base.instance;

import io.automatiko.engine.workflow.base.core.Context;

public interface ContextInstance {

	String getContextType();

	long getContextId();

	ContextInstanceContainer getContextInstanceContainer();

	Context getContext();

	ProcessInstance getProcessInstance();

}