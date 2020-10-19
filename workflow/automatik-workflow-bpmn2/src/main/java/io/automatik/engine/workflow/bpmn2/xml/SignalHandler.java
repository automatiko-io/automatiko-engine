
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

/**
 * This class isn't currently used because we don't really check thrown or
 * caught event content (itemDefiniton references) to see if it matches the
 * definition in the process.
 *
 * </p>
 * In fact, at this moment, the whole <code>&lt;signal&gt;</code> element is
 * ignored because that (specifying event content) is it's only function.
 *
 * </p>
 * This handler is just here for two reasons:
 * <ol>
 * <li>So we can process <code>&lt;signal&gt;</code> elements in process
 * definitions</li>
 * <li>When we do end up actively supporting event content, we'll need the
 * functionality in this class</li>
 * </ol>
 */
public class SignalHandler extends BaseAbstractHandler implements Handler {

    @SuppressWarnings("unchecked")
    public SignalHandler() {
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

        // according to the (Semantic.)xsd, both the name and structureRef are optional
        String id = attrs.getValue("id");
        String name = attrs.getValue("name"); // referred to by the signalEventDefinition.signalRef attr
        String structureRef = attrs.getValue("structureRef");

        ProcessBuildData buildData = (ProcessBuildData) parser.getData();
        Map<String, Signal> signals = (Map<String, Signal>) buildData.getMetaData("Signals");
        if (signals == null) {
            signals = new HashMap<String, Signal>();
            buildData.setMetaData("Signals", signals);
        }

        Signal s = new Signal(id, name, structureRef);
        signals.put(id, s);

        return s;
    }

    public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
        parser.endElementBuilder();
        return parser.getCurrent();
    }

    public Class<?> generateNodeFor() {
        return Error.class;
    }

}
