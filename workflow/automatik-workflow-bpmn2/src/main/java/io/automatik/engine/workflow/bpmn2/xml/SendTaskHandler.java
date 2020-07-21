
package io.automatik.engine.workflow.bpmn2.xml;

import java.util.Map;

import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatik.engine.workflow.bpmn2.core.Message;
import io.automatik.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatik.engine.workflow.compiler.xml.ProcessBuildData;
import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.core.node.WorkItemNode;

public class SendTaskHandler extends TaskHandler {

	protected Node createNode(Attributes attrs) {
		return new WorkItemNode();
	}

	@SuppressWarnings("unchecked")
	public Class generateNodeFor() {
		return Node.class;
	}

	@SuppressWarnings("unchecked")
	protected void handleNode(final Node node, final Element element, final String uri, final String localName,
			final ExtensibleXmlParser parser) throws SAXException {
		super.handleNode(node, element, uri, localName, parser);
		WorkItemNode workItemNode = (WorkItemNode) node;
		String messageRef = element.getAttribute("messageRef");
		Map<String, Message> messages = (Map<String, Message>) ((ProcessBuildData) parser.getData())
				.getMetaData("Messages");
		if (messages == null) {
			throw new IllegalArgumentException("No messages found");
		}
		Message message = messages.get(messageRef);
		if (message == null) {
			throw new IllegalArgumentException("Could not find message " + messageRef);
		}
		workItemNode.getWork().setParameter("MessageType", message.getType());
	}

	protected String getTaskName(final Element element) {
		return "Send Task";
	}

	public void writeNode(Node node, StringBuilder xmlDump, boolean includeMeta) {
		throw new IllegalArgumentException("Writing out should be handled by WorkItemNodeHandler");
	}
}
