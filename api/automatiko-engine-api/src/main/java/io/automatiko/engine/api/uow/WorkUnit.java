
package io.automatiko.engine.api.uow;

import java.util.function.Consumer;

/**
 * Defines individual work unit that can be associated with unit of work
 *
 */
public interface WorkUnit<T> {

	/**
	 * Returns data attached to the work unit
	 * 
	 * @return actual data of the work unit
	 */
	T data();

	/**
	 * Performs action associated with the work unit usually consuming data
	 */
	void perform();

	/**
	 * Optional abort logic associated with the work unit
	 */
	default void abort() {

	}

	/**
	 * Returns desired priority for the work unit execution order
	 * 
	 * @return property as positive number
	 */
	default Integer priority() {
		return 100;
	}

	/**
	 * Creates new WorkUnit that has only action invoked upon completion of the unit
	 * of work
	 * 
	 * @param data   data associated with the work
	 * @param action work to be executed on given data
	 * @return WorkUnit populated with data and action
	 */
	public static <S> WorkUnit<S> create(S data, Consumer<S> action) {
		return new WorkUnit<S>() {

			@Override
			public S data() {
				return data;
			}

			@Override
			public void perform() {
				action.accept(data());
			}

		};
	}

	/**
	 * Creates new WorkUnit that has both action invoked upon completion of the unit
	 * of work and compensation invoked in case of unit of work cancellation.
	 * 
	 * @param data         data associated with the work
	 * @param action       work to be executed on given data
	 * @param compensation revert action to be performed upon cancellation
	 * @return WorkUnit populated with data, action and compensation
	 */
	public static <S> WorkUnit<S> create(S data, Consumer<S> action, Consumer<S> compensation) {
		return new WorkUnit<S>() {

			@Override
			public S data() {
				return data;
			}

			@Override
			public void perform() {
				action.accept(data());
			}

			@Override
			public void abort() {
				compensation.accept(data());
			}
		};
	}
}
