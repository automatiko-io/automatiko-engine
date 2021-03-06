
package io.automatiko.engine.workflow.base.core.context.exception;

import java.util.HashMap;
import java.util.Map;

import io.automatiko.engine.workflow.base.core.Context;
import io.automatiko.engine.workflow.base.core.context.AbstractContext;

public class ExceptionScope extends AbstractContext {

	private static final long serialVersionUID = 510l;

	public static final String EXCEPTION_SCOPE = "ExceptionScope";

	protected Map<String, ExceptionHandler> exceptionHandlers = new HashMap<String, ExceptionHandler>();

	public String getType() {
		return EXCEPTION_SCOPE;
	}

	public void setExceptionHandler(String exception, ExceptionHandler exceptionHandler) {
		this.exceptionHandlers.put(exception, exceptionHandler);
	}

	public ExceptionHandler getExceptionHandler(String exception) {
		ExceptionHandler result = exceptionHandlers.get(exception);
		if (result == null) {
			result = exceptionHandlers.get(null);
		}
		return result;
	}

	public void removeExceptionHandler(String exception) {
		this.exceptionHandlers.remove(exception);
	}

	public Map<String, ExceptionHandler> getExceptionHandlers() {
		return exceptionHandlers;
	}

	public void setExceptionHandlers(Map<String, ExceptionHandler> exceptionHandlers) {
		if (exceptionHandlers == null) {
			throw new IllegalArgumentException("Exception handlers are null");
		}
		this.exceptionHandlers = exceptionHandlers;
	}

	public Context resolveContext(Object param) {
		if (param instanceof String) {
			return getExceptionHandler((String) param) == null ? null : this;
		}
		throw new IllegalArgumentException("ExceptionScopes can only resolve exception names: " + param);
	}

}
