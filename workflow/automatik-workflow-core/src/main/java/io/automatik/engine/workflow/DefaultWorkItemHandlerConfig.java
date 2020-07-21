
package io.automatik.engine.workflow;

import io.automatik.engine.workflow.base.instance.impl.demo.SystemOutWorkItemHandler;
import io.automatik.engine.workflow.base.instance.impl.humantask.HumanTaskWorkItemHandler;

public class DefaultWorkItemHandlerConfig extends CachedWorkItemHandlerConfig {
	{
		register("Log", new SystemOutWorkItemHandler());
		register("Human Task", new HumanTaskWorkItemHandler());
	}
}
