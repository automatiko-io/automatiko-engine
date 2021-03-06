
package io.automatiko.engine.workflow.bpmn2.xml;

import java.util.ArrayList;
import java.util.List;

import io.automatiko.engine.api.definition.process.Connection;
import io.automatiko.engine.workflow.bpmn2.core.Association;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.node.CompositeNode;

public abstract class AbstractCompositeNodeHandler extends AbstractNodeHandler {

	protected void visitConnectionsAndAssociations(Node node, StringBuilder xmlDump, int metaDataType) {
		// add associations
		List<Connection> connections = getSubConnections((CompositeNode) node);
		xmlDump.append("    <!-- connections -->" + EOL);
		for (Connection connection : connections) {
			XmlBPMNProcessDumper.INSTANCE.visitConnection(connection, xmlDump, metaDataType);
		}
		// add associations
		List<Association> associations = (List<Association>) node.getMetaData().get(ProcessHandler.ASSOCIATIONS);
		if (associations != null) {
			for (Association association : associations) {
				XmlBPMNProcessDumper.INSTANCE.visitAssociation(association, xmlDump);
			}
		}
	}

	protected List<Connection> getSubConnections(CompositeNode compositeNode) {
		List<Connection> connections = new ArrayList<Connection>();
		for (io.automatiko.engine.api.definition.process.Node subNode : compositeNode.getNodes()) {
			// filter out composite start and end nodes as they can be regenerated
			if (!(subNode instanceof CompositeNode.CompositeNodeEnd)) {
				for (Connection connection : subNode.getIncomingConnections(Node.CONNECTION_DEFAULT_TYPE)) {
					if (!(connection.getFrom() instanceof CompositeNode.CompositeNodeStart)) {
						connections.add(connection);
					}
				}
			}
		}
		return connections;
	}
}
