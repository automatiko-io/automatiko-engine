
package io.automatiko.engine.workflow.bpmn2.xml;

import java.util.HashSet;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatiko.engine.workflow.bpmn2.core.Interface;
import io.automatiko.engine.workflow.bpmn2.core.Interface.Operation;
import io.automatiko.engine.workflow.compiler.xml.BaseAbstractHandler;
import io.automatiko.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatiko.engine.workflow.compiler.xml.Handler;

public class OperationHandler extends BaseAbstractHandler implements Handler {

	@SuppressWarnings("unchecked")
	public OperationHandler() {
		if ((this.validParents == null) && (this.validPeers == null)) {
			this.validParents = new HashSet();
			this.validParents.add(Interface.class);

			this.validPeers = new HashSet();
			this.validPeers.add(null);
			this.validPeers.add(Operation.class);

			this.allowNesting = false;
		}
	}

	public Object start(final String uri, final String localName, final Attributes attrs,
			final ExtensibleXmlParser parser) throws SAXException {
		parser.startElementBuilder(localName, attrs);

		String id = attrs.getValue("id");
		String name = attrs.getValue("name");
		String implRef = attrs.getValue("implementationRef");

		Interface i = (Interface) parser.getParent();
		Operation operation = i.addOperation(id, name);
		if (implRef != null) {
			operation.setImplementationRef(implRef);
		}
		return operation;
	}

	public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
		parser.endElementBuilder();
		return parser.getCurrent();
	}

	public Class<?> generateNodeFor() {
		return Operation.class;
	}

}
