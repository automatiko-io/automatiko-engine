package io.automatiko.engine.workflow.base.instance.impl.end;

import io.automatiko.engine.api.workflow.EndOfInstanceStrategy;
import io.automatiko.engine.api.workflow.ProcessInstance;

public class KeepEndOfInstanceStrategy implements EndOfInstanceStrategy {

    @Override
    public boolean shouldInstanceBeRemoved() {
        return false;
    }

    @Override
    public boolean shouldInstanceBeUpdated() {
        return true;
    }

    @Override
    public void perform(ProcessInstance<?> instance) {
        // do nothing

    }

}
