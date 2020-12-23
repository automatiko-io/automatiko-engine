
package io.automatiko.engine.workflow.bpmn2.xml;

import org.w3c.dom.Element;
import org.xml.sax.Attributes;

import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.node.WorkItemNode;

public class ManualTaskHandler extends TaskHandler {

	protected Node createNode(Attributes attrs) {
		return new WorkItemNode();
	}

	@SuppressWarnings("unchecked")
	public Class generateNodeFor() {
		return Node.class;
	}

	protected String getTaskName(final Element element) {
		return "Manual Task";
	}

	public void writeNode(Node node, StringBuilder xmlDump, boolean includeMeta) {
		throw new IllegalArgumentException("Writing out should be handled by TaskHandler");
	}
}
