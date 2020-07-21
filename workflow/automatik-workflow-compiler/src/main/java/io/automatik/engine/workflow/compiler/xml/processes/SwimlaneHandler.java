
package io.automatik.engine.workflow.compiler.xml.processes;

import java.util.HashSet;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import io.automatik.engine.api.definition.process.Process;
import io.automatik.engine.workflow.base.core.context.swimlane.Swimlane;
import io.automatik.engine.workflow.base.core.context.swimlane.SwimlaneContext;
import io.automatik.engine.workflow.compiler.xml.BaseAbstractHandler;
import io.automatik.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatik.engine.workflow.compiler.xml.Handler;
import io.automatik.engine.workflow.process.core.impl.WorkflowProcessImpl;

public class SwimlaneHandler extends BaseAbstractHandler implements Handler {
	public SwimlaneHandler() {
		if ((this.validParents == null) && (this.validPeers == null)) {
			this.validParents = new HashSet();
			this.validParents.add(Process.class);

			this.validPeers = new HashSet();
			this.validPeers.add(null);

			this.allowNesting = false;
		}
	}

	public Object start(final String uri, final String localName, final Attributes attrs,
			final ExtensibleXmlParser parser) throws SAXException {
		parser.startElementBuilder(localName, attrs);
		WorkflowProcessImpl process = (WorkflowProcessImpl) parser.getParent();
		final String name = attrs.getValue("name");
		emptyAttributeCheck(localName, "name", name, parser);

		SwimlaneContext swimlaneContext = (SwimlaneContext) process.getDefaultContext(SwimlaneContext.SWIMLANE_SCOPE);
		if (swimlaneContext != null) {
			Swimlane swimlane = new Swimlane();
			swimlane.setName(name);
			swimlaneContext.addSwimlane(swimlane);
		} else {
			throw new SAXParseException("Could not find default swimlane context.", parser.getLocator());
		}

		return null;
	}

	public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
		parser.endElementBuilder();
		return null;
	}

	public Class generateNodeFor() {
		return Swimlane.class;
	}

}
