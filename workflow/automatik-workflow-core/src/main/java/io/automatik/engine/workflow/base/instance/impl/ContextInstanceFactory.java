
package io.automatik.engine.workflow.base.instance.impl;

import io.automatik.engine.workflow.base.core.Context;
import io.automatik.engine.workflow.base.instance.ContextInstance;
import io.automatik.engine.workflow.base.instance.ContextInstanceContainer;
import io.automatik.engine.workflow.base.instance.ProcessInstance;

public interface ContextInstanceFactory {

	ContextInstance getContextInstance(Context context, ContextInstanceContainer contextInstanceContainer,
			ProcessInstance processInstance);

}
