
package io.automatiko.engine.workflow.bpmn2.xml;

import org.xml.sax.Attributes;

import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.node.Join;
import io.automatiko.engine.workflow.process.core.node.Split;

public class ComplexGatewayHandler extends AbstractNodeHandler {

	protected Node createNode(Attributes attrs) {
		final String type = attrs.getValue("gatewayDirection");
		if ("Converging".equals(type)) {
			Join join = new Join();
			join.setType(Join.TYPE_UNDEFINED);
			return join;
		} else if ("Diverging".equals(type)) {
			Split split = new Split();
			split.setType(Split.TYPE_UNDEFINED);
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
