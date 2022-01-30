
package io.automatiko.engine.workflow.process.core.node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.automatiko.engine.api.definition.process.Connection;
import io.automatiko.engine.workflow.base.core.context.variable.Mappable;
import io.automatiko.engine.workflow.base.core.event.EventFilter;
import io.automatiko.engine.workflow.base.core.event.EventTransformer;
import io.automatiko.engine.workflow.base.core.event.EventTypeFilter;
import io.automatiko.engine.workflow.process.core.impl.ExtendedNodeImpl;

public class EventNode extends ExtendedNodeImpl implements EventNodeInterface, Mappable {

    private static final long serialVersionUID = 510l;

    private List<EventFilter> filters = new ArrayList<EventFilter>();
    private EventTransformer transformer;
    private String variableName;
    private String scope;

    private List<DataAssociation> outMapping = new LinkedList<DataAssociation>();

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

    public void addOutMapping(String parameterName, String variableName) {
        outMapping.add(new DataAssociation(parameterName, variableName, null, null));
    }

    public void setOutMappings(Map<String, String> outMapping) {
        this.outMapping = new LinkedList<DataAssociation>();
        for (Map.Entry<String, String> entry : outMapping.entrySet()) {
            addOutMapping(entry.getKey(), entry.getValue());
        }
    }

    public String getOutMapping(String parameterName) {
        return getOutMappings().get(parameterName);
    }

    public Map<String, String> getOutMappings() {
        Map<String, String> out = new HashMap<String, String>();
        for (DataAssociation assoc : outMapping) {
            if (assoc.getSources().size() == 1
                    && (assoc.getAssignments() == null || assoc.getAssignments().size() == 0)
                    && assoc.getTransformation() == null) {
                out.put(assoc.getSources().get(0), assoc.getTarget());
            }
        }
        return out;
    }

    public void addOutAssociation(DataAssociation dataAssociation) {
        outMapping.add(dataAssociation);
    }

    public List<DataAssociation> getOutAssociations() {
        return Collections.unmodifiableList(outMapping);
    }

    public void validateAddIncomingConnection(final String type, final Connection connection) {
        super.validateAddIncomingConnection(type, connection);
        if (!io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
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
        if (!io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
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

    @Override
    public void addInMapping(String parameterName, String variableName) {
        throw new IllegalArgumentException("A start event [" + this.getMetaData("UniqueId") + ", " + this.getName()
                + "] does not support input mappings");
    }

    @Override
    public void setInMappings(Map<String, String> inMapping) {
        throw new IllegalArgumentException("A start event [" + this.getMetaData("UniqueId") + ", " + this.getName()
                + "] does not support input mappings");
    }

    @Override
    public String getInMapping(String parameterName) {
        return null;
    }

    @Override
    public Map<String, String> getInMappings() {
        return Collections.emptyMap();
    }

    @Override
    public void addInAssociation(DataAssociation dataAssociation) {
        throw new IllegalArgumentException("A start event does not support input mappings");
    }

    @Override
    public List<DataAssociation> getInAssociations() {
        return Collections.emptyList();
    }

}
