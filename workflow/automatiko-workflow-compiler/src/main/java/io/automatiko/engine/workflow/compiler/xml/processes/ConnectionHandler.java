
package io.automatiko.engine.workflow.compiler.xml.processes;

import java.util.HashSet;

import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.workflow.compiler.xml.BaseAbstractHandler;
import io.automatiko.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatiko.engine.workflow.compiler.xml.Handler;
import io.automatiko.engine.workflow.process.core.Connection;
import io.automatiko.engine.workflow.process.core.NodeContainer;
import io.automatiko.engine.workflow.process.core.impl.ConnectionImpl;
import io.automatiko.engine.workflow.process.core.impl.NodeImpl;

public class ConnectionHandler extends BaseAbstractHandler implements Handler {
	public ConnectionHandler() {
		if ((this.validParents == null) && (this.validPeers == null)) {
			this.validParents = new HashSet();
			this.validParents.add(NodeContainer.class);

			this.validPeers = new HashSet();
			this.validPeers.add(null);
			this.validPeers.add(Connection.class);

			this.allowNesting = false;
		}
	}

	public Object start(final String uri, final String localName, final Attributes attrs,
			final ExtensibleXmlParser parser) throws SAXException {
		parser.startElementBuilder(localName, attrs);
		String fromId = attrs.getValue("from");
		emptyAttributeCheck(localName, "from", fromId, parser);
		String toId = attrs.getValue("to");
		emptyAttributeCheck(localName, "to", toId, parser);
		String bendpoints = attrs.getValue("bendpoints");

		String fromType = attrs.getValue("fromType");
		if (fromType == null || fromType.trim().length() == 0) {
			fromType = NodeImpl.CONNECTION_DEFAULT_TYPE;
		}
		String toType = attrs.getValue("toType");
		if (toType == null || toType.trim().length() == 0) {
			toType = NodeImpl.CONNECTION_DEFAULT_TYPE;
		}

		NodeContainer nodeContainer = (NodeContainer) parser.getParent();
		Node fromNode = nodeContainer.getNode(new Long(fromId));
		Node toNode = nodeContainer.getNode(new Long(toId));

		if (fromNode == null) {
			throw new SAXParseException("Node '" + fromId + "'cannot be found", parser.getLocator());
		}
		if (toNode == null) {
			throw new SAXParseException("Node '" + toId + "' cannot be found", parser.getLocator());
		}

		ConnectionImpl connection = new ConnectionImpl(fromNode, fromType, toNode, toType);
		connection.setMetaData("bendpoints", bendpoints);

		return connection;
	}

	public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
		final Element element = parser.endElementBuilder();
		return parser.getCurrent();
	}

	public Class generateNodeFor() {
		return Connection.class;
	}

}
