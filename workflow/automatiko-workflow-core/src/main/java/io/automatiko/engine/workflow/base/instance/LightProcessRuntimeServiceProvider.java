
package io.automatiko.engine.workflow.base.instance;

import io.automatiko.engine.services.signal.DefaultSignalManagerHub;
import io.automatiko.engine.services.uow.CollectingUnitOfWorkFactory;
import io.automatiko.engine.services.uow.DefaultUnitOfWorkManager;
import io.automatiko.engine.workflow.DefaultProcessEventListenerConfig;
import io.automatiko.engine.workflow.DefaultWorkItemHandlerConfig;
import io.automatiko.engine.workflow.base.instance.context.variable.DefaultVariableInitializer;

public class LightProcessRuntimeServiceProvider extends AbstractProcessRuntimeServiceProvider {

	public LightProcessRuntimeServiceProvider() {
		super(null, new DefaultWorkItemHandlerConfig(), new DefaultProcessEventListenerConfig(),
				new DefaultSignalManagerHub(), new DefaultUnitOfWorkManager(new CollectingUnitOfWorkFactory()),
				new DefaultVariableInitializer());
	}
}
