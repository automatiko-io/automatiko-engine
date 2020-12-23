
package io.automatiko.engine.services.uow;

import io.automatiko.engine.api.uow.UnitOfWork;
import io.automatiko.engine.api.uow.WorkUnit;

/**
 * The simplest version of unit of work (and one used when no other is
 * configured) that simply pass through the work it intercepts. It has no
 * operation methods for life cycle methods like start, end and abort.
 *
 */
public class PassThroughUnitOfWork implements UnitOfWork {

	@Override
	public void start() {
		// no-op

	}

	@Override
	public void end() {
		// no-op

	}

	@Override
	public void abort() {
		// no-op

	}

	@Override
	public void intercept(WorkUnit work) {
		work.perform();
	}

}
