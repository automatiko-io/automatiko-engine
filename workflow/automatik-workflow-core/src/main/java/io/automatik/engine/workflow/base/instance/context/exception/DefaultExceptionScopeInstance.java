
package io.automatik.engine.workflow.base.instance.context.exception;

import io.automatik.engine.workflow.base.core.context.ProcessContext;
import io.automatik.engine.workflow.base.core.context.exception.ActionExceptionHandler;
import io.automatik.engine.workflow.base.core.context.exception.ExceptionHandler;
import io.automatik.engine.workflow.base.core.context.exception.ExceptionScope;
import io.automatik.engine.workflow.base.instance.ContextInstanceContainer;
import io.automatik.engine.workflow.base.instance.ProcessInstance;
import io.automatik.engine.workflow.base.instance.impl.Action;
import io.automatik.engine.workflow.process.instance.NodeInstance;

public class DefaultExceptionScopeInstance extends ExceptionScopeInstance {

	private static final long serialVersionUID = 510l;

	public String getContextType() {
		return ExceptionScope.EXCEPTION_SCOPE;
	}

	public void handleException(ExceptionHandler handler, String exception, Object params) {

		if (handler instanceof ActionExceptionHandler) {
			ActionExceptionHandler exceptionHandler = (ActionExceptionHandler) handler;
			Action action = (Action) exceptionHandler.getAction().getMetaData("Action");
			try {
				ProcessInstance processInstance = getProcessInstance();
				ProcessContext processContext = new ProcessContext(processInstance.getProcessRuntime());
				ContextInstanceContainer contextInstanceContainer = getContextInstanceContainer();
				if (contextInstanceContainer instanceof NodeInstance) {
					processContext.setNodeInstance((NodeInstance) contextInstanceContainer);
				} else {
					processContext.setProcessInstance(processInstance);
				}
				String faultVariable = exceptionHandler.getFaultVariable();
				if (faultVariable != null) {
					processContext.setVariable(faultVariable, params);
				}
				action.execute(processContext);
			} catch (Exception e) {
				throw new RuntimeException("unable to execute Action", e);
			}
		} else {
			throw new IllegalArgumentException("Unknown exception handler " + handler);
		}
	}

}
