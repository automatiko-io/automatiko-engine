
package io.automatik.engine.workflow.compiler.xml.processes;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import io.automatik.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.core.node.RuleSetNode;

public class RuleSetNodeHandler extends AbstractNodeHandler {

	protected Node createNode() {
		return new RuleSetNode();
	}

	public void handleNode(final Node node, final Element element, final String uri, final String localName,
			final ExtensibleXmlParser parser) throws SAXException {
		super.handleNode(node, element, uri, localName, parser);
		String language = element.getAttribute("implementation");
		if (language == null || language.equalsIgnoreCase("##unspecified") || language.isEmpty()) {
			language = RuleSetNode.DMN_LANG;
		}
	}

	@SuppressWarnings("unchecked")
	public Class generateNodeFor() {
		return RuleSetNode.class;
	}

	public void writeNode(Node node, StringBuilder xmlDump, boolean includeMeta) {
		RuleSetNode ruleSetNode = (RuleSetNode) node;
		writeNode("ruleSet", ruleSetNode, xmlDump, includeMeta);
		RuleSetNode.RuleType ruleType = ruleSetNode.getRuleType();
		if (ruleType != null) {
			if (!ruleType.isDecision()) {
				xmlDump.append("ruleFlowGroup=\"" + ruleType.getName() + "\" ");
			}
		}
		xmlDump.append(" implementation=\"" + ruleSetNode.getLanguage() + "\" ");
		if (ruleSetNode.getTimers() != null || (includeMeta && containsMetaData(ruleSetNode))) {
			xmlDump.append(">\n");
			if (ruleSetNode.getTimers() != null) {
				writeTimers(ruleSetNode.getTimers(), xmlDump);
			}
			if (includeMeta) {
				writeMetaData(ruleSetNode, xmlDump);
			}
			endNode("ruleSet", xmlDump);
		} else {
			endNode(xmlDump);
		}
	}

}
