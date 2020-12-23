
package io.automatiko.engine.services.uow;

import java.util.function.Consumer;

import io.automatiko.engine.api.uow.WorkUnit;
import io.automatiko.engine.api.workflow.ProcessInstance;

public class ProcessInstanceWorkUnit<T> implements WorkUnit<ProcessInstance<T>> {

	private ProcessInstance<T> data;
	private Consumer<Object> action;
	private Consumer<Object> compensation;

	public ProcessInstanceWorkUnit(ProcessInstance<T> data, Consumer<Object> action) {
		this.data = data;
		this.action = action;
	}

	public ProcessInstanceWorkUnit(ProcessInstance<T> data, Consumer<Object> action, Consumer<Object> compensation) {
		this.data = data;
		this.action = action;
		this.compensation = compensation;
	}

	@Override
	public ProcessInstance<T> data() {
		return data;
	}

	@Override
	public void perform() {
		action.accept(data());
	}

	@Override
	public void abort() {
		if (compensation != null) {
			compensation.accept(data());
		}
	}

	@Override
	public Integer priority() {
		return 10;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ProcessInstanceWorkUnit<T> other = (ProcessInstanceWorkUnit) obj;
		if (data == null) {
			if (other.data != null)
				return false;
		} else if (!data.equals(other.data))
			return false;
		return true;
	}

}
