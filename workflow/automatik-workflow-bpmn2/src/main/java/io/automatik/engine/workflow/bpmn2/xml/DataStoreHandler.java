package io.automatik.engine.workflow.bpmn2.xml;

import static io.automatik.engine.workflow.compiler.util.ClassUtils.constructClass;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatik.engine.workflow.base.core.datatype.DataType;
import io.automatik.engine.workflow.base.core.datatype.impl.type.ObjectDataType;
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

public class DataStoreHandler extends BaseAbstractHandler implements Handler {

	@SuppressWarnings("rawtypes")
	public DataStoreHandler() {
		if ((this.validParents == null) && (this.validPeers == null)) {
			this.validParents = new HashSet<Class<?>>();
			this.validParents.add(Definitions.class);

			this.validPeers = new HashSet<Class<?>>();
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
		DataStore store = new DataStore();
		store.setId(attrs.getValue("id"));
		store.setName(attrs.getValue("name"));
		final String itemSubjectRef = attrs.getValue("itemSubjectRef");
		store.setItemSubjectRef(itemSubjectRef);
		Map<String, ItemDefinition> itemDefinitions = (Map<String, ItemDefinition>) ((ProcessBuildData) parser
				.getData()).getMetaData("ItemDefinitions");
		// retrieve type from item definition
		// FIXME we bypass namespace resolving here. That's not a good idea when we
		// start having several documents, with imports.
		String localItemSubjectRef = itemSubjectRef.substring(itemSubjectRef.indexOf(":") + 1);
		DataType dataType = new ObjectDataType();
		if (itemDefinitions != null) {
			ItemDefinition itemDefinition = itemDefinitions.get(localItemSubjectRef);
			if (itemDefinition != null) {
				dataType = new ObjectDataType(constructClass(itemDefinition.getStructureRef(), parser.getClassLoader()),
						itemDefinition.getStructureRef());
			}
		}
		store.setType(dataType);

		Definitions parent = (Definitions) parser.getParent();
		List<DataStore> dataStores = parent.getDataStores();
		if (dataStores == null) {
			dataStores = new ArrayList<DataStore>();
			parent.setDataStores(dataStores);
		}
		dataStores.add(store);
		return store;
	}

	@SuppressWarnings("unchecked")
	public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
		parser.endElementBuilder();
		return parser.getCurrent();
	}

	public Class<?> generateNodeFor() {
		return DataStore.class;
	}

}
