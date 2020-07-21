
package io.automatik.engine.workflow.bpmn2.xml;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatik.engine.workflow.bpmn2.core.DataStore;
import io.automatik.engine.workflow.bpmn2.core.Definitions;
import io.automatik.engine.workflow.bpmn2.core.Error;
import io.automatik.engine.workflow.bpmn2.core.Escalation;
import io.automatik.engine.workflow.bpmn2.core.Interface;
import io.automatik.engine.workflow.bpmn2.core.ItemDefinition;
import io.automatik.engine.workflow.bpmn2.core.Message;
import io.automatik.engine.workflow.bpmn2.core.Signal;
import io.automatik.engine.workflow.compiler.xml.BaseAbstractHandler;
import io.automatik.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatik.engine.workflow.compiler.xml.Handler;
import io.automatik.engine.workflow.compiler.xml.ProcessBuildData;
import io.automatik.engine.workflow.process.executable.core.ExecutableProcess;

public class ErrorHandler extends BaseAbstractHandler implements Handler {

	@SuppressWarnings("unchecked")
	public ErrorHandler() {
		if ((this.validParents == null) && (this.validPeers == null)) {
			this.validParents = new HashSet();
			this.validParents.add(Definitions.class);

			this.validPeers = new HashSet();
			this.validPeers.add(null);
			this.validPeers.add(ItemDefinition.class);
			this.validPeers.add(Message.class);
			this.validPeers.add(Interface.class);
			this.validPeers.add(Escalation.class);
			this.validPeers.add(Error.class);
			this.validPeers.add(Signal.class);
			this.validPeers.add(DataStore.class);
			this.validPeers.add(ExecutableProcess.class);

			this.allowNesting = false;
		}
	}

	@SuppressWarnings("unchecked")
	public Object start(final String uri, final String localName, final Attributes attrs,
			final ExtensibleXmlParser parser) throws SAXException {
		parser.startElementBuilder(localName, attrs);

		String id = attrs.getValue("id");
		String errorCode = attrs.getValue("errorCode");
		String structureRef = attrs.getValue("structureRef");

		Definitions definitions = (Definitions) parser.getParent();

		List<Error> errors = definitions.getErrors();
		if (errors == null) {
			errors = new ArrayList<Error>();
			definitions.setErrors(errors);
			((ProcessBuildData) parser.getData()).setMetaData("Errors", errors);
		}
		Error e = new Error(id, errorCode, structureRef);
		errors.add(e);

		return e;
	}

	public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
		parser.endElementBuilder();
		return parser.getCurrent();
	}

	public Class<?> generateNodeFor() {
		return Error.class;
	}

}
