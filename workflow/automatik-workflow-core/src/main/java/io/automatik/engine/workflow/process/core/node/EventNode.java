
package io.automatik.engine.workflow.process.core.node;

import java.util.ArrayList;
import java.util.List;

import io.automatik.engine.api.definition.process.Connection;
import io.automatik.engine.workflow.base.core.event.EventFilter;
import io.automatik.engine.workflow.base.core.event.EventTransformer;
import io.automatik.engine.workflow.base.core.event.EventTypeFilter;
import io.automatik.engine.workflow.process.core.impl.ExtendedNodeImpl;

public class EventNode extends ExtendedNodeImpl implements EventNodeInterface {

    private static final long serialVersionUID = 510l;

    private List<EventFilter> filters = new ArrayList<EventFilter>();
    private EventTransformer transformer;
    private String variableName;
    private String scope;

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public void addEventFilter(EventFilter eventFilter) {
        filters.add(eventFilter);
    }

    public void removeEventFilter(EventFilter eventFilter) {
        filters.remove(eventFilter);
    }

    public List<EventFilter> getEventFilters() {
        return filters;
    }

    public void setEventFilters(List<EventFilter> filters) {
        this.filters = filters;
    }

    public String getType() {
        for (EventFilter filter : filters) {
            if (filter instanceof EventTypeFilter) {
                return ((EventTypeFilter) filter).getType();
            }
        }
        return null;
    }

    public boolean acceptsEvent(String type, Object event) {
        for (EventFilter filter : filters) {
            if (!filter.acceptsEvent(type, event)) {
                return false;
            }
        }
        return true;
    }

    public void setEventTransformer(EventTransformer transformer) {
        this.transformer = transformer;
    }

    public EventTransformer getEventTransformer() {
        return transformer;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public boolean isDynamic() {
        return "true".equals(getMetaData("customAutoStart"));
    }

    public void validateAddIncomingConnection(final String type, final Connection connection) {
        super.validateAddIncomingConnection(type, connection);
        if (!io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
            throw new IllegalArgumentException("This type of node [" + connection.getTo().getMetaData().get("UniqueId")
                    + ", " + connection.getTo().getName() + "] only accepts default incoming connection type!");
        }
        if (getFrom() != null && !"true".equals(System.getProperty("jbpm.enable.multi.con"))) {
            throw new IllegalArgumentException("This type of node [" + connection.getTo().getMetaData().get("UniqueId")
                    + ", " + connection.getTo().getName() + "] cannot have more than one incoming connection!");
        }
    }

    public void validateAddOutgoingConnection(final String type, final Connection connection) {
        super.validateAddOutgoingConnection(type, connection);
        if (!io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
            throw new IllegalArgumentException(
                    "This type of node [" + connection.getFrom().getMetaData().get("UniqueId") + ", "
                            + connection.getFrom().getName() + "] only accepts default outgoing connection type!");
        }
        if (getTo() != null && !"true".equals(System.getProperty("jbpm.enable.multi.con"))) {
            throw new IllegalArgumentException(
                    "This type of node [" + connection.getFrom().getMetaData().get("UniqueId") + ", "
                            + connection.getFrom().getName() + "] cannot have more than one outgoing connection!");
        }
    }

}
