
package io.automatik.engine.services.uow;

import io.automatik.engine.api.event.EventManager;
import io.automatik.engine.api.uow.UnitOfWork;
import io.automatik.engine.api.uow.UnitOfWorkFactory;

public class CollectingUnitOfWorkFactory implements UnitOfWorkFactory {

	@Override
	public UnitOfWork create(EventManager eventManager) {
		return new CollectingUnitOfWork(eventManager);
	}

}
