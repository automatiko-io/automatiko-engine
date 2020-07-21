
package io.automatik.engine.workflow.bpmn2.xml;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import io.automatik.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatik.engine.workflow.process.core.Node;

public class TransactionHandler extends SubProcessHandler {

	protected void handleNode(final Node node, final Element element, final String uri, final String localName,
			final ExtensibleXmlParser parser) throws SAXException {
		super.handleNode(node, element, uri, localName, parser);
		node.setMetaData("Transaction", true);
	}

	public void writeNode(Node node, StringBuilder xmlDump, boolean includeMeta) {
		throw new IllegalArgumentException("Writing out should be handled by specific handlers");
	}

}
