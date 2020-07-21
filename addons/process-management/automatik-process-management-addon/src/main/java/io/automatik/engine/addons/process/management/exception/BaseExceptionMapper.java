
package io.automatik.engine.addons.process.management.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public abstract class BaseExceptionMapper<E extends Throwable> implements ExceptionMapper<E> {

	ExceptionsHandler exceptionsHandler;

	public BaseExceptionMapper() {
		this.exceptionsHandler = new ExceptionsHandler();
	}

	@SuppressWarnings("squid:S3038")
	public abstract Response toResponse(E e);
}
