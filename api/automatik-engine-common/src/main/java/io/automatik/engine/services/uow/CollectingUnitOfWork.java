
package io.automatik.engine.services.uow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.automatik.engine.api.event.EventBatch;
import io.automatik.engine.api.event.EventManager;
import io.automatik.engine.api.uow.UnitOfWork;
import io.automatik.engine.api.uow.WorkUnit;

/**
 * Simple unit of work that collects work elements throughout the life of the
 * unit and invokes all of them at the end when end method is invoked. It does
 * not invoke the work when abort is invoked, only clears the collected items.
 *
 */
public class CollectingUnitOfWork implements UnitOfWork {

	private Set<WorkUnit<?>> collectedWork;
	private boolean done;

	private final EventManager eventManager;

	public CollectingUnitOfWork(EventManager eventManager) {
		this.eventManager = eventManager;
	}

	@Override
	public void start() {
		checkDone();
		if (collectedWork == null) {
			collectedWork = new LinkedHashSet<>();
		}
	}

	@Override
	public void end() {
		checkStarted();
		EventBatch batch = eventManager.newBatch();

		for (WorkUnit<?> work : sorted()) {
			batch.append(work.data());
			work.perform();
		}
		eventManager.publish(batch);
		done();
	}

	@Override
	public void abort() {
		checkStarted();
		for (WorkUnit<?> work : sorted()) {
			work.abort();
		}
		done();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void intercept(WorkUnit work) {
		checkStarted();
		if (work == null) {
			throw new NullPointerException("Work must be non null");
		}
		collectedWork.remove(work);
		collectedWork.add(work);
	}

	protected Collection<WorkUnit<?>> sorted() {
		List<WorkUnit<?>> sortedCollectedWork = new ArrayList<>(collectedWork);
		sortedCollectedWork.sort((u1, u2) -> u1.priority().compareTo(u2.priority()));

		return sortedCollectedWork;
	}

	protected void checkDone() {
		if (done) {
			throw new IllegalStateException("Unit of work is already done (ended or aborted)");
		}
	}

	protected void checkStarted() {
		if (collectedWork == null) {
			throw new IllegalStateException("Unit of work is not started");
		}
	}

	protected void done() {
		done = true;
		collectedWork = null;
	}
}
