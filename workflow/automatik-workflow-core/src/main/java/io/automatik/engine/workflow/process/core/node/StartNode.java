
package io.automatik.engine.workflow.process.core.node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.automatik.engine.api.definition.process.Connection;
import io.automatik.engine.workflow.base.core.context.variable.Mappable;
import io.automatik.engine.workflow.base.core.event.EventTransformer;
import io.automatik.engine.workflow.base.core.timer.Timer;
import io.automatik.engine.workflow.process.core.impl.ExtendedNodeImpl;

/**
 * Default implementation of a start node.
 * 
 */
public class StartNode extends ExtendedNodeImpl implements Mappable {

	private static final String[] EVENT_TYPES = new String[] { EVENT_NODE_EXIT };

	private static final long serialVersionUID = 510l;

	private List<Trigger> triggers;

	private boolean isInterrupting;

	private List<DataAssociation> outMapping = new LinkedList<DataAssociation>();

	private Timer timer;

	private EventTransformer transformer;

	public void addTrigger(Trigger trigger) {
		if (triggers == null) {
			triggers = new ArrayList<Trigger>();
		}
		triggers.add(trigger);
	}

	public void removeTrigger(Trigger trigger) {
		if (triggers != null) {
			triggers.remove(trigger);
		}
	}

	public List<Trigger> getTriggers() {
		return triggers;
	}

	public void setTriggers(List<Trigger> triggers) {
		this.triggers = triggers;
	}

	public String[] getActionTypes() {
		return EVENT_TYPES;
	}

	public void validateAddIncomingConnection(final String type, final Connection connection) {
		throw new UnsupportedOperationException("A start node [" + this.getMetaData("UniqueId") + ", " + this.getName()
				+ "] may not have an incoming connection!");
	}

	public void validateRemoveIncomingConnection(final String type, final Connection connection) {
		throw new UnsupportedOperationException("A start node [" + this.getMetaData("UniqueId") + ", " + this.getName()
				+ "] may not have an incoming connection!");
	}

	public void validateAddOutgoingConnection(final String type, final Connection connection) {
		super.validateAddOutgoingConnection(type, connection);
		if (!io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
			throw new IllegalArgumentException("A start node [" + this.getMetaData("UniqueId") + ", " + this.getName()
					+ "] only accepts default outgoing connection type!");
		}
		if (getTo() != null && !"true".equals(System.getProperty("jbpm.enable.multi.con"))) {
			throw new IllegalArgumentException("A start node [" + this.getMetaData("UniqueId") + ", " + this.getName()
					+ "] cannot have more than one outgoing connection!");
		}
	}

	public boolean isInterrupting() {
		return isInterrupting;
	}

	public void setInterrupting(boolean isInterrupting) {
		this.isInterrupting = isInterrupting;
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
			if (assoc.getSources().size() == 1 && (assoc.getAssignments() == null || assoc.getAssignments().size() == 0)
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

	public Timer getTimer() {
		return timer;
	}

	public void setTimer(Timer timer) {
		this.timer = timer;
	}

	public void setEventTransformer(EventTransformer transformer) {
		this.transformer = transformer;
	}

	public EventTransformer getEventTransformer() {
		return transformer;
	}

}
