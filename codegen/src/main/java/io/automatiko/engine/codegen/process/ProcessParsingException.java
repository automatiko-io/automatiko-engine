
package io.automatiko.engine.codegen.process;

public class ProcessParsingException extends RuntimeException {

	public ProcessParsingException(Throwable cause) {
		super(cause);
	}

	public ProcessParsingException(String s, Throwable e) {
		super(s, e);
	}
}
