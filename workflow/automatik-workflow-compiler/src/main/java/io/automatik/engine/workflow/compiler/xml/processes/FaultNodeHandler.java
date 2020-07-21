
package io.automatik.engine.workflow.compiler.xml.processes;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import io.automatik.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.core.node.FaultNode;

public class FaultNodeHandler extends AbstractNodeHandler {

	protected Node createNode() {
		return new FaultNode();
	}

	public void handleNode(final Node node, final Element element, final String uri, final String localName,
			final ExtensibleXmlParser parser) throws SAXException {
		super.handleNode(node, element, uri, localName, parser);
		FaultNode faultNode = (FaultNode) node;
		String faultName = element.getAttribute("faultName");
		if (faultName != null && faultName.length() != 0) {
			faultNode.setFaultName(faultName);
		}
		String faultVariable = element.getAttribute("faultVariable");
		if (faultVariable != null && !"".equals(faultVariable)) {
			faultNode.setFaultVariable(faultVariable);
		}
	}

	public Class generateNodeFor() {
		return FaultNode.class;
	}

	public void writeNode(Node node, StringBuilder xmlDump, boolean includeMeta) {
		FaultNode faultNode = (FaultNode) node;
		writeNode("fault", faultNode, xmlDump, includeMeta);
		String faultName = faultNode.getFaultName();
		if (faultName != null && faultName.length() != 0) {
			xmlDump.append("faultName=\"" + faultName + "\" ");
		}
		String faultVariable = faultNode.getFaultVariable();
		if (faultVariable != null && faultVariable.length() != 0) {
			xmlDump.append("faultVariable=\"" + faultVariable + "\" ");
		}
		if (includeMeta && containsMetaData(faultNode)) {
			xmlDump.append(">" + EOL);
			writeMetaData(faultNode, xmlDump);
			endNode("fault", xmlDump);
		} else {
			endNode(xmlDump);
		}
	}

}
