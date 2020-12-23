
package io.automatiko.engine.workflow.bpmn2.xml;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatiko.engine.workflow.bpmn2.core.DataStore;
import io.automatiko.engine.workflow.bpmn2.core.Definitions;
import io.automatiko.engine.workflow.bpmn2.core.Error;
import io.automatiko.engine.workflow.bpmn2.core.Escalation;
import io.automatiko.engine.workflow.bpmn2.core.Interface;
import io.automatiko.engine.workflow.bpmn2.core.ItemDefinition;
import io.automatiko.engine.workflow.bpmn2.core.Message;
import io.automatiko.engine.workflow.bpmn2.core.Resource;
import io.automatiko.engine.workflow.bpmn2.core.Signal;
import io.automatiko.engine.workflow.compiler.xml.BaseAbstractHandler;
import io.automatiko.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatiko.engine.workflow.compiler.xml.Handler;
import io.automatiko.engine.workflow.compiler.xml.ProcessBuildData;
import io.automatiko.engine.workflow.process.executable.core.ExecutableProcess;

public class InterfaceHandler extends BaseAbstractHandler implements Handler {

    @SuppressWarnings("unchecked")
    public InterfaceHandler() {
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
        String name = attrs.getValue("name");
        String implRef = attrs.getValue("implementationRef");

        if (name == null || name.isEmpty()) {
            throw new MalformedNodeException(id, name, "interface name is a required attribute");
        }

        ProcessBuildData buildData = (ProcessBuildData) parser.getData();
        List<Interface> interfaces = (List<Interface>) buildData.getMetaData("Interfaces");
        if (interfaces == null) {
            interfaces = new ArrayList<Interface>();
            buildData.setMetaData("Interfaces", interfaces);
        }
        Interface i = new Interface(id, name);
        if (implRef != null) {
            i.setImplementationRef(implRef);
        }
        interfaces.add(i);
        return i;
    }

    public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
        parser.endElementBuilder();
        return parser.getCurrent();
    }

    public Class<?> generateNodeFor() {
        return Interface.class;
    }

}
