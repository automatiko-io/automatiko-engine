package io.automatiko.engine.workflow.base.instance.impl.end;

import io.automatiko.engine.api.workflow.EndOfInstanceStrategy;
import io.automatiko.engine.api.workflow.ProcessInstance;

public class RemoveEndOfInstanceStrategy implements EndOfInstanceStrategy {

    @Override
    public boolean shouldInstanceBeRemoved() {
        return true;
    }

    @Override
    public boolean shouldInstanceBeUpdated() {
        return false;
    }

    @Override
    public void perform(ProcessInstance<?> instance) {
        // do nothing
    }

}
