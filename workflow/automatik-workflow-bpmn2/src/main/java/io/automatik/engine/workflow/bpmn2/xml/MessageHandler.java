
package io.automatik.engine.workflow.bpmn2.xml;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatik.engine.workflow.bpmn2.core.DataStore;
import io.automatik.engine.workflow.bpmn2.core.Definitions;
import io.automatik.engine.workflow.bpmn2.core.Error;
import io.automatik.engine.workflow.bpmn2.core.Escalation;
import io.automatik.engine.workflow.bpmn2.core.Interface;
import io.automatik.engine.workflow.bpmn2.core.ItemDefinition;
import io.automatik.engine.workflow.bpmn2.core.Message;
import io.automatik.engine.workflow.bpmn2.core.Resource;
import io.automatik.engine.workflow.bpmn2.core.Signal;
import io.automatik.engine.workflow.compiler.xml.BaseAbstractHandler;
import io.automatik.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatik.engine.workflow.compiler.xml.Handler;
import io.automatik.engine.workflow.compiler.xml.ProcessBuildData;
import io.automatik.engine.workflow.process.executable.core.ExecutableProcess;

public class MessageHandler extends BaseAbstractHandler implements Handler {

    @SuppressWarnings("unchecked")
    public MessageHandler() {
        if ((this.validParents == null) && (this.validPeers == null)) {
            this.validParents = new HashSet();
            this.validParents.add(Definitions.class);

            this.validPeers = new HashSet();
            this.validPeers.add(null);
            this.validPeers.add(ItemDefinition.class);
            this.validPeers.add(Resource.class);
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
        String itemRef = attrs.getValue("itemRef");
        String name = attrs.getValue("name");
        if (name == null) {
            name = id;
        }

        Map<String, ItemDefinition> itemDefinitions = (Map<String, ItemDefinition>) ((ProcessBuildData) parser
                .getData()).getMetaData("ItemDefinitions");
        if (itemDefinitions == null) {
            throw new IllegalArgumentException("No item definitions found");
        }
        ItemDefinition itemDefinition = itemDefinitions.get(itemRef);
        if (itemDefinition == null) {
            throw new IllegalArgumentException("Could not find itemDefinition " + itemRef);
        }

        ProcessBuildData buildData = (ProcessBuildData) parser.getData();
        Map<String, Message> messages = (Map<String, Message>) ((ProcessBuildData) parser.getData())
                .getMetaData("Messages");
        if (messages == null) {
            messages = new HashMap<String, Message>();
            buildData.setMetaData("Messages", messages);
        }
        Message message = new Message(id);
        message.setType(itemDefinition.getStructureRef());
        message.setName(name);

        if (message.getType() != null && !message.getType().isEmpty()) {
            messages.put(id, message);
        }
        return message;
    }

    public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
        parser.endElementBuilder();
        return parser.getCurrent();
    }

    public Class<?> generateNodeFor() {
        return Message.class;
    }

}
