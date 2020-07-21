
package io.automatik.engine.api.runtime.process;

public class ProcessWorkItemHandlerException extends RuntimeException {

	private static final long serialVersionUID = -5953387125605633663L;

	public enum HandlingStrategy {
		RETRY, COMPLETE, ABORT, RETHROW
	}

	private String processId;
	private HandlingStrategy strategy;

	public ProcessWorkItemHandlerException(String processId, String strategy, Throwable cause) {
		this(processId, HandlingStrategy.valueOf(strategy), cause);
	}

	public ProcessWorkItemHandlerException(String processId, HandlingStrategy strategy, Throwable cause) {
		super(cause);
		this.processId = processId;
		this.strategy = strategy;
		if (processId == null || strategy == null) {
			throw new IllegalArgumentException("Process id and strategy are required");
		}
	}

	public String getProcessId() {
		return processId;
	}

	public HandlingStrategy getStrategy() {
		return strategy;
	}

}
