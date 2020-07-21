
package io.automatik.engine.workflow.base.instance;

import io.automatik.engine.services.signal.DefaultSignalManagerHub;
import io.automatik.engine.services.uow.CollectingUnitOfWorkFactory;
import io.automatik.engine.services.uow.DefaultUnitOfWorkManager;
import io.automatik.engine.workflow.DefaultProcessEventListenerConfig;
import io.automatik.engine.workflow.DefaultWorkItemHandlerConfig;

public class LightProcessRuntimeServiceProvider extends AbstractProcessRuntimeServiceProvider {

	public LightProcessRuntimeServiceProvider() {
		super(null, new DefaultWorkItemHandlerConfig(), new DefaultProcessEventListenerConfig(),
				new DefaultSignalManagerHub(), new DefaultUnitOfWorkManager(new CollectingUnitOfWorkFactory()));
	}
}
