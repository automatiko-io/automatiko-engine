package io.automatiko.engine.workflow.bpmn2.xml;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatiko.engine.workflow.bpmn2.core.Bpmn2Import;
import io.automatiko.engine.workflow.bpmn2.core.DataStore;
import io.automatiko.engine.workflow.bpmn2.core.Definitions;
import io.automatiko.engine.workflow.bpmn2.core.Error;
import io.automatiko.engine.workflow.bpmn2.core.Escalation;
import io.automatiko.engine.workflow.bpmn2.core.Interface;
import io.automatiko.engine.workflow.bpmn2.core.ItemDefinition;
import io.automatiko.engine.workflow.bpmn2.core.Message;
import io.automatiko.engine.workflow.bpmn2.core.Signal;
import io.automatiko.engine.workflow.compiler.xml.BaseAbstractHandler;
import io.automatiko.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatiko.engine.workflow.compiler.xml.Handler;
import io.automatiko.engine.workflow.compiler.xml.ProcessBuildData;
import io.automatiko.engine.workflow.process.executable.core.ExecutableProcess;

public class Bpmn2ImportHandler extends BaseAbstractHandler implements Handler {

	public Bpmn2ImportHandler() {
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

	public Object start(final String uri, final String localName, final Attributes attrs,
			final ExtensibleXmlParser parser) throws SAXException {
		parser.startElementBuilder(localName, attrs);

		final String type = attrs.getValue("importType");
		final String location = attrs.getValue("location");
		final String namespace = attrs.getValue("namespace");
		ProcessBuildData buildData = (ProcessBuildData) parser.getData();

		if (type != null && location != null && namespace != null) {
			List<Bpmn2Import> typedImports = (List<Bpmn2Import>) buildData.getMetaData("Bpmn2Imports");
			if (typedImports == null) {
				typedImports = new ArrayList<Bpmn2Import>();
				buildData.setMetaData("Bpmn2Imports", typedImports);
			}
			typedImports.add(new Bpmn2Import(type, location, namespace));
		}
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
