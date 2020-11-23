
package io.automatik.engine.workflow.base.instance.context.exception;

import io.automatik.engine.api.runtime.process.NodeInstance;
import io.automatik.engine.workflow.base.core.context.exception.ExceptionHandler;
import io.automatik.engine.workflow.base.core.context.exception.ExceptionScope;
import io.automatik.engine.workflow.base.instance.context.AbstractContextInstance;

public abstract class ExceptionScopeInstance extends AbstractContextInstance {

    private static final long serialVersionUID = 510l;

    public String getContextType() {
        return ExceptionScope.EXCEPTION_SCOPE;
    }

    public ExceptionScope getExceptionScope() {
        return (ExceptionScope) getContext();
    }

    public void handleException(NodeInstance nodeInstance, String exception, Object params) {
        ExceptionHandler handler = getExceptionScope().getExceptionHandler(exception);
        if (handler == null) {
            throw new IllegalArgumentException("Could not find ExceptionHandler for " + exception);
        }
        handleException(nodeInstance, handler, exception, params);
    }

    public abstract void handleException(NodeInstance nodeInstance, ExceptionHandler handler, String exception, Object params);

}
