
package io.automatik.engine.workflow.compiler.xml.processes;

import static io.automatik.engine.workflow.process.executable.core.Metadata.COMPLETION_CONDITION;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import io.automatik.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.core.node.DynamicNode;

public class DynamicNodeHandler extends CompositeNodeHandler {

	public static final String AUTOCOMPLETE_COMPLETION_CONDITION = "autocomplete";

	protected Node createNode() {
		return new DynamicNode();
	}

	public Class<?> generateNodeFor() {
		return DynamicNode.class;
	}

	protected String getNodeName() {
		return "dynamic";
	}

	@Override
	protected void handleNode(Node node, Element element, String uri, String localName, ExtensibleXmlParser parser)
			throws SAXException {
		super.handleNode(node, element, uri, localName, parser);
		DynamicNode dynamicNode = (DynamicNode) node;
		for (int i = 0; i < element.getChildNodes().getLength(); i++) {
			org.w3c.dom.Node n = element.getChildNodes().item(i);
			if (COMPLETION_CONDITION.equals(n.getNodeName())) {
				if (AUTOCOMPLETE_COMPLETION_CONDITION.equals(n.getTextContent())) {
					dynamicNode.setAutoComplete(true);
				} else {
					dynamicNode.setCompletionCondition(n.getTextContent());
				}
			}
		}
	}
}
