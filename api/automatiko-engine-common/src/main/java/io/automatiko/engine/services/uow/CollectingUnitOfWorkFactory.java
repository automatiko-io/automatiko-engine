
package io.automatiko.engine.services.uow;

import io.automatiko.engine.api.event.EventManager;
import io.automatiko.engine.api.uow.UnitOfWork;
import io.automatiko.engine.api.uow.UnitOfWorkFactory;

public class CollectingUnitOfWorkFactory implements UnitOfWorkFactory {

	@Override
	public UnitOfWork create(EventManager eventManager) {
		return new CollectingUnitOfWork(eventManager);
	}

}
