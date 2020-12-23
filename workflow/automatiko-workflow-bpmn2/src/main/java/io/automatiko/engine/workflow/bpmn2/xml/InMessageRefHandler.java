
package io.automatiko.engine.workflow.bpmn2.xml;

import java.util.HashSet;
import java.util.Map;

import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatiko.engine.workflow.bpmn2.core.Message;
import io.automatiko.engine.workflow.bpmn2.core.Interface.Operation;
import io.automatiko.engine.workflow.compiler.xml.BaseAbstractHandler;
import io.automatiko.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatiko.engine.workflow.compiler.xml.Handler;
import io.automatiko.engine.workflow.compiler.xml.ProcessBuildData;

public class InMessageRefHandler extends BaseAbstractHandler implements Handler {

	@SuppressWarnings("unchecked")
	public InMessageRefHandler() {
		if ((this.validParents == null) && (this.validPeers == null)) {
			this.validParents = new HashSet();
			this.validParents.add(Operation.class);

			this.validPeers = new HashSet();
			this.validPeers.add(null);

			this.allowNesting = false;
		}
	}

	public Object start(final String uri, final String localName, final Attributes attrs,
			final ExtensibleXmlParser parser) throws SAXException {
		parser.startElementBuilder(localName, attrs);
		return null;
	}

	@SuppressWarnings("unchecked")
	public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
		Element element = parser.endElementBuilder();
		String messageId = element.getTextContent();
		Map<String, Message> messages = (Map<String, Message>) ((ProcessBuildData) parser.getData())
				.getMetaData("Messages");
		if (messages == null) {
			throw new IllegalArgumentException("No messages found");
		}
		Operation operation = (Operation) parser.getParent();
		Message message = messages.get(messageId);
		if (message != null) {
			operation.setMessage(message);
		}

		return parser.getCurrent();
	}

	public Class<?> generateNodeFor() {
		return Message.class;
	}

}
