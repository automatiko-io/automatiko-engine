
package io.automatiko.engine.workflow.bpmn2.xml;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

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

public class ItemDefinitionHandler extends BaseAbstractHandler implements Handler {

	@SuppressWarnings("unchecked")
	public ItemDefinitionHandler() {
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
		String type = attrs.getValue("structureRef");

		ProcessBuildData buildData = (ProcessBuildData) parser.getData();
		Map<String, ItemDefinition> itemDefinitions = (Map<String, ItemDefinition>) buildData
				.getMetaData("ItemDefinitions");
		if (itemDefinitions == null) {
			itemDefinitions = new HashMap<String, ItemDefinition>();
			buildData.setMetaData("ItemDefinitions", itemDefinitions);
		}
		ItemDefinition itemDefinition = new ItemDefinition(id);
		itemDefinition.setStructureRef(type);
		itemDefinitions.put(id, itemDefinition);
		return itemDefinition;
	}

	public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
		parser.endElementBuilder();
		return parser.getCurrent();
	}

	public Class<?> generateNodeFor() {
		return ItemDefinition.class;
	}

}
