
package io.automatiko.engine.workflow.base.instance.impl;

import io.automatiko.engine.workflow.base.core.Context;
import io.automatiko.engine.workflow.base.instance.ContextInstance;
import io.automatiko.engine.workflow.base.instance.ContextInstanceContainer;
import io.automatiko.engine.workflow.base.instance.ProcessInstance;

public interface ContextInstanceFactory {

	ContextInstance getContextInstance(Context context, ContextInstanceContainer contextInstanceContainer,
			ProcessInstance processInstance);

}
