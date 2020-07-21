
package io.automatik.engine.workflow.bpmn2.xml;

import org.xml.sax.Attributes;

import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.core.node.Join;

public class JoinHandler extends AbstractNodeHandler {

	protected Node createNode(Attributes attrs) {
		throw new IllegalArgumentException("Reading in should be handled by gateway handler");
	}

	@SuppressWarnings("unchecked")
	public Class generateNodeFor() {
		return Join.class;
	}

	public void writeNode(Node node, StringBuilder xmlDump, int metaDataType) {
		Join join = (Join) node;
		String type = null;
		switch (join.getType()) {
		case Join.TYPE_AND:
			type = "parallelGateway";
			break;
		case Join.TYPE_XOR:
			type = "exclusiveGateway";
			break;
		case Join.TYPE_OR:
			type = "inclusiveGateway";
			break;
		default:
			type = "complexGateway";
		}
		writeNode(type, node, xmlDump, metaDataType);
		xmlDump.append("gatewayDirection=\"Converging\" >" + EOL);
		writeExtensionElements(join, xmlDump);
		endNode(type, xmlDump);
	}

}
