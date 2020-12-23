
package io.automatiko.engine.workflow.process.core.node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.automatiko.engine.workflow.base.core.timer.Timer;
import io.automatiko.engine.workflow.process.core.ProcessAction;
import io.automatiko.engine.workflow.process.core.impl.ExtendedNodeImpl;

public class StateBasedNode extends ExtendedNodeImpl {

    private static final long serialVersionUID = 510l;

    private Map<Timer, ProcessAction> timers;

    private List<String> boundaryEvents;

    public Map<Timer, ProcessAction> getTimers() {
        return timers;
    }

    public void addTimer(Timer timer, ProcessAction action) {
        if (timers == null) {
            timers = new HashMap<Timer, ProcessAction>();
        }
        if (timer.getId() == 0) {
            long id = 0;
            for (Timer t : timers.keySet()) {
                if (t.getId() > id) {
                    id = t.getId();
                }
            }
            timer.setId(++id);
        }
        timers.put(timer, action);
    }

    public void removeAllTimers() {
        if (timers != null) {
            timers.clear();
        }
    }

    public void addBoundaryEvents(String boundaryEvent) {
        if (this.boundaryEvents == null) {
            this.boundaryEvents = new ArrayList<String>();
        }
        this.boundaryEvents.add(boundaryEvent);
    }

    public void setBoundaryEvents(List<String> boundaryEvents) {
        this.boundaryEvents = boundaryEvents;
    }

    public List<String> getBoundaryEvents() {
        return boundaryEvents;
    }

}
