
package io.automatik.engine.workflow.compiler.xml.processes;

import java.util.HashSet;

import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import io.automatik.engine.api.definition.process.Process;
import io.automatik.engine.workflow.base.core.ContextContainer;
import io.automatik.engine.workflow.base.core.context.exception.ActionExceptionHandler;
import io.automatik.engine.workflow.base.core.context.exception.ExceptionScope;
import io.automatik.engine.workflow.compiler.xml.BaseAbstractHandler;
import io.automatik.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatik.engine.workflow.compiler.xml.Handler;
import io.automatik.engine.workflow.process.core.ProcessAction;

public class ExceptionHandlerHandler extends BaseAbstractHandler implements Handler {

	public ExceptionHandlerHandler() {
		if ((this.validParents == null) && (this.validPeers == null)) {
			this.validParents = new HashSet<Class<?>>();
			this.validParents.add(Process.class);

			this.validPeers = new HashSet<Class<?>>();
			this.validPeers.add(null);

			this.allowNesting = false;
		}
	}

	public Object start(final String uri, final String localName, final Attributes attrs,
			final ExtensibleXmlParser parser) throws SAXException {
		parser.startElementBuilder(localName, attrs);
		return null;
	}

	public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
		final Element element = parser.endElementBuilder();
		ContextContainer contextContainer = (ContextContainer) parser.getParent();

		final String type = element.getAttribute("type");
		emptyAttributeCheck(localName, "type", type, parser);

		final String faultName = element.getAttribute("faultName");
		emptyAttributeCheck(localName, "faultName", type, parser);

		final String faultVariable = element.getAttribute("faultVariable");

		ActionExceptionHandler exceptionHandler = null;
		if ("action".equals(type)) {
			exceptionHandler = new ActionExceptionHandler();
			org.w3c.dom.Node xmlNode = element.getFirstChild();
			if (xmlNode instanceof Element) {
				Element actionXml = (Element) xmlNode;
				ProcessAction action = ActionNodeHandler.extractAction(actionXml);
				((ActionExceptionHandler) exceptionHandler).setAction(action);
			}
		} else {
			throw new SAXParseException("Unknown exception handler type " + type, parser.getLocator());
		}

		if (faultVariable != null && faultVariable.length() > 0) {
			exceptionHandler.setFaultVariable(faultVariable);
		}

		ExceptionScope exceptionScope = (ExceptionScope) contextContainer
				.getDefaultContext(ExceptionScope.EXCEPTION_SCOPE);
		if (exceptionScope == null) {
			exceptionScope = new ExceptionScope();
			contextContainer.addContext(exceptionScope);
			contextContainer.setDefaultContext(exceptionScope);
		}

		exceptionScope.setExceptionHandler(faultName, exceptionHandler);

		return null;
	}

	public Class<?> generateNodeFor() {
		return null;
	}

}
