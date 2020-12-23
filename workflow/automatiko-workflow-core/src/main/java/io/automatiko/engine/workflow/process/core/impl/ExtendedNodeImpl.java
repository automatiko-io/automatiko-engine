
package io.automatiko.engine.workflow.process.core.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.automatiko.engine.workflow.process.core.ProcessAction;

public class ExtendedNodeImpl extends NodeImpl {

    public static final String EVENT_NODE_ENTER = "onEntry";
    public static final String EVENT_NODE_EXIT = "onExit";

    private static final String[] EVENT_TYPES = new String[] { EVENT_NODE_ENTER, EVENT_NODE_EXIT };

    private static final long serialVersionUID = 510l;

    private Map<String, List<ProcessAction>> actions = new HashMap<String, List<ProcessAction>>();

    public void setActions(String type, List<ProcessAction> actions) {
        this.actions.put(type, actions);
    }

    public List<ProcessAction> getActions(String type) {
        return this.actions.get(type);
    }

    public boolean containsActions() {
        for (List<ProcessAction> l : actions.values()) {
            if (!l.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public String[] getActionTypes() {
        return EVENT_TYPES;
    }

}
