
package io.automatiko.engine.workflow.compiler.xml.processes;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import io.automatiko.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.ProcessAction;
import io.automatiko.engine.workflow.process.core.impl.ConsequenceAction;
import io.automatiko.engine.workflow.process.core.node.ActionNode;

public class ActionNodeHandler extends AbstractNodeHandler {

	protected Node createNode() {
		return new ActionNode();
	}

	public void handleNode(final Node node, final Element element, final String uri, final String localName,
			final ExtensibleXmlParser parser) throws SAXException {
		super.handleNode(node, element, uri, localName, parser);
		ActionNode actionNode = (ActionNode) node;
		org.w3c.dom.Node xmlNode = element.getFirstChild();
		if (xmlNode instanceof Element) {
			Element actionXml = (Element) xmlNode;
			ProcessAction action = extractAction(actionXml);
			actionNode.setAction(action);
		}
	}

	@SuppressWarnings("unchecked")
	public Class generateNodeFor() {
		return ActionNode.class;
	}

	public void writeNode(Node node, StringBuilder xmlDump, boolean includeMeta) {
		ActionNode actionNode = (ActionNode) node;
		writeNode("actionNode", actionNode, xmlDump, includeMeta);
		ConsequenceAction action = (ConsequenceAction) actionNode.getAction();
		if (action != null || (includeMeta && containsMetaData(actionNode))) {
			xmlDump.append(">" + EOL);
			if (action != null) {
				writeAction(action, xmlDump);
			}
			if (includeMeta) {
				writeMetaData(actionNode, xmlDump);
			}
			endNode("actionNode", xmlDump);
		} else {
			endNode(xmlDump);
		}
	}

}
