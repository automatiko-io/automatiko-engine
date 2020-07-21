
package io.automatik.engine.workflow.bpmn2.xml;

import org.xml.sax.Attributes;

import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.core.node.Split;

public class EventBasedGatewayHandler extends AbstractNodeHandler {

	protected Node createNode(Attributes attrs) {
		final String type = attrs.getValue("gatewayDirection");
		if ("Diverging".equals(type)) {
			Split split = new Split();
			split.setType(Split.TYPE_XAND);
			split.setMetaData("EventBased", "true");
			return split;
		} else {
			throw new IllegalArgumentException("Unknown gateway direction: " + type);
		}
	}

	@SuppressWarnings("unchecked")
	public Class generateNodeFor() {
		return Node.class;
	}

	public void writeNode(Node node, StringBuilder xmlDump, int metaDataType) {
		throw new IllegalArgumentException("Writing out should be handled by split / join handler");
	}

}
