
package io.automatiko.engine.workflow;

import io.automatiko.engine.workflow.base.instance.impl.demo.SystemOutWorkItemHandler;
import io.automatiko.engine.workflow.base.instance.impl.humantask.HumanTaskWorkItemHandler;

public class DefaultWorkItemHandlerConfig extends CachedWorkItemHandlerConfig {
	{
		register("Log", new SystemOutWorkItemHandler());
		register("Human Task", new HumanTaskWorkItemHandler());
	}
}
