
package io.automatiko.engine.workflow.process.executable.core.factory;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import io.automatiko.engine.api.runtime.process.ProcessContext;
import io.automatiko.engine.workflow.base.core.context.variable.Mappable;
import io.automatiko.engine.workflow.base.core.event.EventTypeFilter;
import io.automatiko.engine.workflow.base.core.timer.Timer;
import io.automatiko.engine.workflow.base.instance.impl.Action;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.NodeContainer;
import io.automatiko.engine.workflow.process.core.ProcessAction;
import io.automatiko.engine.workflow.process.core.node.Assignment;
import io.automatiko.engine.workflow.process.core.node.CompositeContextNode;
import io.automatiko.engine.workflow.process.core.node.DataAssociation;
import io.automatiko.engine.workflow.process.core.node.EventTrigger;
import io.automatiko.engine.workflow.process.core.node.StartNode;
import io.automatiko.engine.workflow.process.executable.core.ExecutableNodeContainerFactory;

public class StartNodeFactory extends ExtendedNodeFactory implements MappableNodeFactory {

    public static final String METHOD_INTERRUPTING = "interrupting";
    public static final String METHOD_TRIGGER = "trigger";
    public static final String METHOD_TIMER = "timer";

    public StartNodeFactory(ExecutableNodeContainerFactory nodeContainerFactory, NodeContainer nodeContainer, long id) {
        super(nodeContainerFactory, nodeContainer, id);
    }

    protected Node createNode() {
        return new StartNode();
    }

    protected StartNode getStartNode() {
        return (StartNode) getNode();
    }

    @Override
    public StartNodeFactory name(String name) {
        super.name(name);
        return this;
    }

    public StartNodeFactory interrupting(boolean interrupting) {
        getStartNode().setInterrupting(interrupting);
        return this;
    }

    public StartNodeFactory trigger(String triggerEventType, String mapping) {
        return trigger(triggerEventType, mapping, null);
    }

    public StartNodeFactory trigger(String triggerEventType, String mapping, String variableName) {
        EventTrigger trigger = new EventTrigger();
        EventTypeFilter eventFilter = new EventTypeFilter();
        eventFilter.setType(triggerEventType);
        trigger.addEventFilter(eventFilter);
        if (mapping != null && !mapping.isEmpty()) {
            trigger.addInMapping(mapping, variableName);
        }
        getStartNode().addTrigger(trigger);
        return this;
    }

    public StartNodeFactory timer(String delay, String period, String date, int timeType) {
        Timer timer = new Timer();
        timer.setDate(date);
        timer.setDelay(delay);
        timer.setPeriod(period);
        timer.setTimeType(timeType);

        getStartNode().setTimer(timer);

        if (nodeContainer instanceof CompositeContextNode) {
            ProcessAction noop = new ProcessAction();

            Action action = kcontext -> {
            };
            noop.wire(action);
            ((CompositeContextNode) nodeContainer).addTimer(timer, noop);
        }
        return this;
    }

    public StartNodeFactory outMapping(String source, String target, String assignmentDialect, String assignmentFrom,
            String assignmentTo) {
        List<Assignment> assignments = null;
        if (assignmentFrom != null && assignmentTo != null) {
            assignments = Arrays.asList(new Assignment(assignmentDialect, assignmentFrom, assignmentTo));
        }
        DataAssociation dataAssociation = new DataAssociation(source, target, assignments, null);
        getStartNode().addOutAssociation(dataAssociation);
        return this;
    }

    public StartNodeFactory condition(Predicate<ProcessContext> condition) {
        getStartNode().setCondition(condition);
        return this;
    }

    @Override
    public Mappable getMappableNode() {
        return getStartNode();
    }
}
