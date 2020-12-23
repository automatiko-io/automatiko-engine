
package io.automatiko.engine.workflow.compiler.xml.processes;

import java.util.HashSet;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatiko.engine.workflow.compiler.xml.BaseAbstractHandler;
import io.automatiko.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatiko.engine.workflow.compiler.xml.Handler;
import io.automatiko.engine.workflow.process.core.node.CompositeNode;

public class OutPortHandler extends BaseAbstractHandler implements Handler {
	public OutPortHandler() {
		if ((this.validParents == null) && (this.validPeers == null)) {
			this.validParents = new HashSet();
			this.validParents.add(CompositeNode.class);

			this.validPeers = new HashSet();
			this.validPeers.add(null);

			this.allowNesting = false;
		}
	}

	public Object start(final String uri, final String localName, final Attributes attrs,
			final ExtensibleXmlParser parser) throws SAXException {
		parser.startElementBuilder(localName, attrs);
		CompositeNode compositeNode = (CompositeNode) parser.getParent();
		final String type = attrs.getValue("type");
		emptyAttributeCheck(localName, "type", type, parser);
		final String nodeId = attrs.getValue("nodeId");
		emptyAttributeCheck(localName, "nodeId", nodeId, parser);
		final String nodeOutType = attrs.getValue("nodeOutType");
		emptyAttributeCheck(localName, "nodeOutType", nodeOutType, parser);
		compositeNode.linkOutgoingConnections(new Long(nodeId), nodeOutType, type);
		return null;
	}

	public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
		parser.endElementBuilder();
		return null;
	}

	public Class generateNodeFor() {
		return null;
	}

}
