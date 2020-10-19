
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

public class EscalationHandler extends BaseAbstractHandler implements Handler {

    @SuppressWarnings("unchecked")
    public EscalationHandler() {
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
        String escalationCode = attrs.getValue("escalationCode");

        ProcessBuildData buildData = (ProcessBuildData) parser.getData();
        Map<String, Escalation> escalations = (Map<String, Escalation>) buildData
                .getMetaData(ProcessHandler.ESCALATIONS);
        if (escalations == null) {
            escalations = new HashMap<String, Escalation>();
            buildData.setMetaData(ProcessHandler.ESCALATIONS, escalations);
        }
        Escalation e = new Escalation(id, escalationCode);
        escalations.put(id, e);
        return e;
    }

    public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
        parser.endElementBuilder();
        return parser.getCurrent();
    }

    public Class<?> generateNodeFor() {
        return Escalation.class;
    }

}
