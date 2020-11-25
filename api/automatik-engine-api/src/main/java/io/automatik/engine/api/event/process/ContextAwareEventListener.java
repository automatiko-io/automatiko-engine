
package io.automatik.engine.api.event.process;

import java.util.function.Consumer;

public class ContextAwareEventListener extends DefaultProcessEventListener {

    private final String identifier;

    private final Consumer<ContextAwareEventListener> action;

    private ContextAwareEventListener(String identifier, Consumer<ContextAwareEventListener> action) {
        this.identifier = identifier;
        this.action = action;
    }

    @Override
    public void afterNodeLeft(ProcessNodeLeftEvent event) {
        action.accept(this);
    }

    @Override
    public void afterVariableChanged(ProcessVariableChangedEvent event) {
        action.accept(this);
    }

    public static ProcessEventListener using(String identifier, Consumer<ContextAwareEventListener> action) {
        return new ContextAwareEventListener(identifier, action);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ContextAwareEventListener other = (ContextAwareEventListener) obj;
        if (identifier == null) {
            if (other.identifier != null)
                return false;
        } else if (!identifier.equals(other.identifier))
            return false;
        return true;
    }
}