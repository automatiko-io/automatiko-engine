
package io.automatiko.engine.workflow.process.core.node;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.automatiko.engine.api.definition.process.Connection;
import io.automatiko.engine.workflow.process.core.impl.ExtendedNodeImpl;

/**
 * Default implementation of a fault node.
 * 
 */
public class FaultNode extends ExtendedNodeImpl {

    private static final String[] EVENT_TYPES = new String[] { EVENT_NODE_ENTER };

    private static final long serialVersionUID = 510l;

    private String errorName;
    private String faultName;
    private String faultVariable;
    private String structureRef;
    private boolean terminateParent = false;

    private List<DataAssociation> inMapping = new LinkedList<DataAssociation>();

    public String getErrorName() {
        return errorName;
    }

    public void setErrorName(String errorName) {
        this.errorName = errorName;
    }

    public String getFaultVariable() {
        return faultVariable;
    }

    public void setFaultVariable(String faultVariable) {
        this.faultVariable = faultVariable;
    }

    public String getFaultName() {
        return faultName;
    }

    public void setFaultName(String faultName) {
        this.faultName = faultName;
    }

    public String getStructureRef() {
        return structureRef;
    }

    public void setStructureRef(String structureRef) {
        this.structureRef = structureRef;
    }

    public boolean isTerminateParent() {
        return terminateParent;
    }

    public void setTerminateParent(boolean terminateParent) {
        this.terminateParent = terminateParent;
    }

    public String[] getActionTypes() {
        return EVENT_TYPES;
    }

    public void addInMapping(String parameterName, String variableName) {
        inMapping.add(new DataAssociation(variableName, parameterName, null, null));
    }

    public void setInMappings(Map<String, String> inMapping) {
        this.inMapping = new LinkedList<DataAssociation>();
        for (Map.Entry<String, String> entry : inMapping.entrySet()) {
            addInMapping(entry.getKey(), entry.getValue());
        }
    }

    public String getInMapping(String parameterName) {
        return getInMappings().get(parameterName);
    }

    public Map<String, String> getInMappings() {
        Map<String, String> in = new HashMap<String, String>();
        for (DataAssociation a : inMapping) {
            if (a.getSources().size() == 1 && (a.getAssignments() == null || a.getAssignments().size() == 0)
                    && a.getTransformation() == null) {
                in.put(a.getTarget(), a.getSources().get(0));
            }
        }
        return in;
    }

    public void addInAssociation(DataAssociation dataAssociation) {
        inMapping.add(dataAssociation);
    }

    public List<DataAssociation> getInAssociations() {
        return Collections.unmodifiableList(inMapping);
    }

    public void validateAddIncomingConnection(final String type, final Connection connection) {
        super.validateAddIncomingConnection(type, connection);
        if (!io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
            throw new IllegalArgumentException("This type of node [" + connection.getTo().getMetaData().get("UniqueId")
                    + ", " + connection.getTo().getName() + "] only accepts default incoming connection type!");
        }
        if (getFrom() != null) {
            throw new IllegalArgumentException("This type of node [" + connection.getTo().getMetaData().get("UniqueId")
                    + ", " + connection.getTo().getName() + "] cannot have more than one incoming connection!");
        }
    }

    public void validateAddOutgoingConnection(final String type, final Connection connection) {
        throw new UnsupportedOperationException("A fault node does not have an outgoing connection!");
    }

    public void validateRemoveOutgoingConnection(final String type, final Connection connection) {
        throw new UnsupportedOperationException("A fault node does not have an outgoing connection!");
    }
}
