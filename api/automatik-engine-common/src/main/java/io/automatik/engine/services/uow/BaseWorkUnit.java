
package io.automatik.engine.services.uow;

import java.util.function.Consumer;

import io.automatik.engine.api.uow.WorkUnit;

public class BaseWorkUnit implements WorkUnit<Object> {

	private Object data;
	private Consumer<Object> action;
	private Consumer<Object> compensation;

	public BaseWorkUnit(Object data, Consumer<Object> action) {
		this.data = data;
		this.action = action;
	}

	public BaseWorkUnit(Object data, Consumer<Object> action, Consumer<Object> compensation) {
		this.data = data;
		this.action = action;
		this.compensation = compensation;
	}

	@Override
	public Object data() {
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

}
