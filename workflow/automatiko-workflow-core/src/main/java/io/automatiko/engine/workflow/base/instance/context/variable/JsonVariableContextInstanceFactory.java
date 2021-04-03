package io.automatiko.engine.workflow.base.instance.context.variable;

import io.automatiko.engine.workflow.base.core.Context;
import io.automatiko.engine.workflow.base.instance.ContextInstance;
import io.automatiko.engine.workflow.base.instance.ContextInstanceContainer;
import io.automatiko.engine.workflow.base.instance.ProcessInstance;
import io.automatiko.engine.workflow.base.instance.context.AbstractContextInstance;
import io.automatiko.engine.workflow.base.instance.impl.ContextInstanceFactory;

public class JsonVariableContextInstanceFactory implements ContextInstanceFactory {

    @Override
    public ContextInstance getContextInstance(Context context, ContextInstanceContainer contextInstanceContainer,
            ProcessInstance processInstance) {

        ContextInstance result = contextInstanceContainer.getContextInstance(context.getType(), context.getId());
        if (result != null) {
            return result;
        }
        AbstractContextInstance contextInstance = new JsonVariableScopeInstance();
        contextInstance.setProcessInstance(processInstance);
        contextInstance.setContextId(context.getId());
        contextInstance.setContextInstanceContainer(contextInstanceContainer);
        contextInstanceContainer.addContextInstance(context.getType(), contextInstance);
        return contextInstance;
    }

}
