
package io.automatiko.engine.workflow.bpmn2.xml;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatiko.engine.api.runtime.process.DataTransformer;
import io.automatiko.engine.workflow.base.core.event.EventFilter;
import io.automatiko.engine.workflow.base.core.event.EventTransformerImpl;
import io.automatiko.engine.workflow.base.core.event.EventTypeFilter;
import io.automatiko.engine.workflow.base.core.impl.DataTransformerRegistry;
import io.automatiko.engine.workflow.base.core.timer.Timer;
import io.automatiko.engine.workflow.bpmn2.core.IntermediateLink;
import io.automatiko.engine.workflow.bpmn2.core.ItemDefinition;
import io.automatiko.engine.workflow.bpmn2.core.Message;
import io.automatiko.engine.workflow.bpmn2.core.Signal;
import io.automatiko.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatiko.engine.workflow.compiler.xml.ProcessBuildData;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.NodeContainer;
import io.automatiko.engine.workflow.process.core.node.CatchLinkNode;
import io.automatiko.engine.workflow.process.core.node.CompositeNode;
import io.automatiko.engine.workflow.process.core.node.EventNode;
import io.automatiko.engine.workflow.process.core.node.StateNode;
import io.automatiko.engine.workflow.process.core.node.TimerNode;
import io.automatiko.engine.workflow.process.core.node.Transformation;
import io.automatiko.engine.workflow.process.executable.core.ExecutableProcess;

public class IntermediateCatchEventHandler extends AbstractNodeHandler {

    private DataTransformerRegistry transformerRegistry = DataTransformerRegistry.get();

    public static final String LINK_NAME = "LinkName";

    protected Node createNode(Attributes attrs) {
        return new EventNode();
    }

    @SuppressWarnings("unchecked")
    public Class generateNodeFor() {
        return EventNode.class;
    }

    public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
        final Element element = parser.endElementBuilder();
        Node node = (Node) parser.getCurrent();
        // determine type of event definition, so the correct type of node
        // can be generated
        org.w3c.dom.Node xmlNode = element.getFirstChild();
        while (xmlNode != null) {
            String nodeName = xmlNode.getNodeName();
            if ("signalEventDefinition".equals(nodeName)) {
                // reuse already created EventNode
                handleSignalNode(node, element, uri, localName, parser);
                node.setMetaData(EVENT_TYPE, "signal");
                node.setMetaData("functionFlowContinue", "true");
                break;
            } else if ("messageEventDefinition".equals(nodeName)) {
                // reuse already created EventNode
                handleMessageNode(node, element, uri, localName, parser);
                node.setMetaData(EVENT_TYPE, "message");
                node.setMetaData("functionFlowContinue", "true");
                break;
            } else if ("timerEventDefinition".equals(nodeName)) {
                // create new timerNode
                TimerNode timerNode = new TimerNode();
                timerNode.setId(node.getId());
                timerNode.setName(node.getName());
                timerNode.setMetaData("UniqueId", node.getMetaData().get("UniqueId"));
                node = timerNode;
                node.setMetaData(EVENT_TYPE, "timer");
                handleTimerNode(node, element, uri, localName, parser);
                node.setMetaData("functionFlowContinue", "true");
                break;
            } else if ("conditionalEventDefinition".equals(nodeName)) {
                // create new stateNode
                StateNode stateNode = new StateNode();
                stateNode.setId(node.getId());
                stateNode.setName(node.getName());
                stateNode.setMetaData("UniqueId", node.getMetaData().get("UniqueId"));
                node = stateNode;
                node.setMetaData(EVENT_TYPE, "conditional");
                handleStateNode(node, element, uri, localName, parser);
                node.setMetaData("functionFlowContinue", "true");
                break;
            } else if ("linkEventDefinition".equals(nodeName)) {
                CatchLinkNode linkNode = new CatchLinkNode();
                linkNode.setId(node.getId());
                node = linkNode;
                node.setMetaData(EVENT_TYPE, "link");
                handleLinkNode(element, node, xmlNode, parser);
                break;
            }
            xmlNode = xmlNode.getNextSibling();
        }
        node.setMetaData("DataOutputs", new LinkedHashMap<String, String>(dataOutputTypes));

        NodeContainer nodeContainer = (NodeContainer) parser.getParent();
        nodeContainer.addNode(node);
        ((ProcessBuildData) parser.getData()).addNode(node);
        return node;
    }

    protected void handleLinkNode(Element element, Node node, org.w3c.dom.Node xmlLinkNode,
            ExtensibleXmlParser parser) {
        NodeContainer nodeContainer = (NodeContainer) parser.getParent();

        node.setName(element.getAttribute("name"));

        NamedNodeMap linkAttr = xmlLinkNode.getAttributes();
        String name = linkAttr.getNamedItem("name").getNodeValue();
        String id = element.getAttribute("id");

        node.setMetaData("UniqueId", id);
        node.setMetaData(LINK_NAME, name);

        org.w3c.dom.Node xmlNode = xmlLinkNode.getFirstChild();

        IntermediateLink aLink = new IntermediateLink();
        aLink.setName(name);
        aLink.setUniqueId(id);

        while (null != xmlNode) {
            String nodeName = xmlNode.getNodeName();
            if ("target".equals(nodeName)) {
                String target = xmlNode.getTextContent();
                node.setMetaData("target", target);
                aLink.setTarget(target);
            }
            if ("source".equals(nodeName)) {
                String source = xmlNode.getTextContent();
                node.setMetaData("source", source);
                aLink.addSource(source);
            }
            xmlNode = xmlNode.getNextSibling();
        }

        if (nodeContainer instanceof ExecutableProcess) {
            ExecutableProcess process = (ExecutableProcess) nodeContainer;
            List<IntermediateLink> links = (List<IntermediateLink>) process.getMetaData().get(ProcessHandler.LINKS);
            if (null == links) {
                links = new ArrayList<IntermediateLink>();
            }
            links.add(aLink);
            process.setMetaData(ProcessHandler.LINKS, links);
        } else if (nodeContainer instanceof CompositeNode) {
            CompositeNode subprocess = (CompositeNode) nodeContainer;
            List<IntermediateLink> links = (List<IntermediateLink>) subprocess.getMetaData().get(ProcessHandler.LINKS);
            if (null == links) {
                links = new ArrayList<IntermediateLink>();
            }
            links.add(aLink);
            subprocess.setMetaData(ProcessHandler.LINKS, links);
        }
    }

    protected void handleSignalNode(final Node node, final Element element, final String uri, final String localName,
            final ExtensibleXmlParser parser) throws SAXException {
        super.handleNode(node, element, uri, localName, parser);
        EventNode eventNode = (EventNode) node;
        org.w3c.dom.Node xmlNode = element.getFirstChild();
        while (xmlNode != null) {
            String nodeName = xmlNode.getNodeName();
            if ("dataOutput".equals(nodeName)) {
                String id = ((Element) xmlNode).getAttribute("id");
                String outputName = ((Element) xmlNode).getAttribute("name");
                dataOutputs.put(id, outputName);

                populateDataOutputs(xmlNode, outputName, parser);
            } else if ("dataOutputAssociation".equals(nodeName)) {
                readDataOutputAssociation(xmlNode, eventNode);
            } else if ("signalEventDefinition".equals(nodeName)) {
                String type = ((Element) xmlNode).getAttribute("signalRef");
                if (type != null && type.trim().length() > 0) {
                    Signal signal = findSignalByName(parser, type);
                    if (signal != null) {
                        eventNode.setMetaData("MessageType", retrieveDataType(signal.getStructureRef(), null, parser));
                    }
                    type = checkSignalAndConvertToRealSignalNam(parser, type);

                    List<EventFilter> eventFilters = new ArrayList<EventFilter>();
                    EventTypeFilter eventFilter = new EventTypeFilter();
                    eventFilter.setType(type);
                    eventFilters.add(eventFilter);
                    eventNode.setEventFilters(eventFilters);
                }
            }
            xmlNode = xmlNode.getNextSibling();
        }
    }

    @SuppressWarnings("unchecked")
    protected void handleMessageNode(final Node node, final Element element, final String uri, final String localName,
            final ExtensibleXmlParser parser) throws SAXException {
        super.handleNode(node, element, uri, localName, parser);
        EventNode eventNode = (EventNode) node;
        org.w3c.dom.Node xmlNode = element.getFirstChild();
        while (xmlNode != null) {
            String nodeName = xmlNode.getNodeName();
            String id = ((Element) xmlNode).getAttribute("id");
            String name = ((Element) xmlNode).getAttribute("name");
            if ("dataOutput".equals(nodeName)) {
                dataOutputs.put(id, name);

                populateDataOutputs(xmlNode, name, parser);
            } else if ("dataOutputAssociation".equals(nodeName)) {
                readDataOutputAssociation(xmlNode, eventNode);
            } else if ("messageEventDefinition".equals(nodeName)) {
                String messageRef = ((Element) xmlNode).getAttribute("messageRef");
                Map<String, Message> messages = (Map<String, Message>) ((ProcessBuildData) parser.getData())
                        .getMetaData("Messages");
                if (messages == null) {
                    throw new IllegalArgumentException("No messages found");
                }
                Message message = messages.get(messageRef);
                if (message == null) {
                    throw new MalformedNodeException(id, name,
                            MessageFormat.format("Could not find message \"{0}\"", messageRef));
                }
                eventNode.setMetaData("MessageType", message.getType());
                eventNode.setMetaData("TriggerType", "ConsumeMessage");
                eventNode.setMetaData("TriggerRef", message.getName());
                eventNode.setMetaData("TriggerCorrelation", message.getCorrelation());
                eventNode.setMetaData("TriggerCorrelationExpr", message.getCorrelationExpression());

                for (Entry<String, Object> entry : message.getMetaData().entrySet()) {
                    eventNode.setMetaData(entry.getKey(), entry.getValue());
                }

                List<EventFilter> eventFilters = new ArrayList<EventFilter>();
                EventTypeFilter eventFilter = new EventTypeFilter();
                eventFilter.setType("Message-" + message.getName());
                eventFilters.add(eventFilter);
                eventNode.setEventFilters(eventFilters);
            }
            xmlNode = xmlNode.getNextSibling();
        }
    }

    protected void handleTimerNode(final Node node, final Element element, final String uri, final String localName,
            final ExtensibleXmlParser parser) throws SAXException {
        super.handleNode(node, element, uri, localName, parser);
        TimerNode timerNode = (TimerNode) node;
        org.w3c.dom.Node xmlNode = element.getFirstChild();
        while (xmlNode != null) {
            String nodeName = xmlNode.getNodeName();
            if ("timerEventDefinition".equals(nodeName)) {
                Timer timer = new Timer();
                org.w3c.dom.Node subNode = xmlNode.getFirstChild();
                while (subNode instanceof Element) {
                    String subNodeName = subNode.getNodeName();
                    if ("timeCycle".equals(subNodeName)) {
                        String delay = subNode.getTextContent();
                        int index = delay.indexOf("###");
                        if (index != -1) {
                            String period = delay.substring(index + 3);
                            delay = delay.substring(0, index);
                            timer.setPeriod(period);
                        }
                        timer.setTimeType(Timer.TIME_CYCLE);
                        timer.setDelay(delay);
                        break;
                    } else if ("timeDuration".equals(subNodeName)) {
                        String delay = subNode.getTextContent();
                        timer.setTimeType(Timer.TIME_DURATION);
                        timer.setDelay(delay);
                        break;
                    } else if ("timeDate".equals(subNodeName)) {
                        String date = subNode.getTextContent();
                        timer.setTimeType(Timer.TIME_DATE);
                        timer.setDate(date);
                        break;
                    }
                    subNode = subNode.getNextSibling();
                }
                timerNode.setTimer(timer);
            }
            xmlNode = xmlNode.getNextSibling();
        }
    }

    protected void handleStateNode(final Node node, final Element element, final String uri, final String localName,
            final ExtensibleXmlParser parser) throws SAXException {
        super.handleNode(node, element, uri, localName, parser);
        StateNode stateNode = (StateNode) node;
        org.w3c.dom.Node xmlNode = element.getFirstChild();
        while (xmlNode != null) {
            String nodeName = xmlNode.getNodeName();
            if ("conditionalEventDefinition".equals(nodeName)) {
                org.w3c.dom.Node subNode = xmlNode.getFirstChild();
                while (subNode != null) {
                    String subnodeName = subNode.getNodeName();
                    if ("condition".equals(subnodeName)) {
                        String condition = xmlNode.getTextContent();
                        stateNode.setMetaData("Condition", condition);
                        break;
                    }
                    subNode = subNode.getNextSibling();
                }
            }
            xmlNode = xmlNode.getNextSibling();
        }
    }

    protected void readDataOutputAssociation(org.w3c.dom.Node xmlNode, EventNode eventNode) {
        // sourceRef
        org.w3c.dom.Node subNode = xmlNode.getFirstChild();
        String from = subNode.getTextContent();

        String to = null;
        Transformation transformation = null;
        // targetRef
        subNode = subNode.getNextSibling();
        if (subNode != null && "targetRef".equals(subNode.getNodeName())) {
            to = subNode.getTextContent();
            eventNode.setVariableName(to);

            subNode = subNode.getNextSibling();
        }
        if (subNode != null && "transformation".equals(subNode.getNodeName())) {
            String lang = subNode.getAttributes().getNamedItem("language").getNodeValue();
            String expression = subNode.getTextContent();
            DataTransformer transformer = transformerRegistry.find(lang);
            if (transformer == null) {
                throw new IllegalArgumentException("No transformer registered for language " + lang);
            }
            transformation = new Transformation(lang, expression, dataOutputs.get(from));
            eventNode.setMetaData("Transformation", transformation);

            eventNode.setEventTransformer(new EventTransformerImpl(transformation));
        } else if (subNode != null && "assignment".equals(subNode.getNodeName())) {

            // assignments
            while (subNode != null) {
                org.w3c.dom.Node ssubNode = subNode.getFirstChild();
                to = ssubNode.getNextSibling().getTextContent();

                subNode = subNode.getNextSibling();

                eventNode.setVariableName(to);
            }

        }
    }

    protected void populateDataOutputs(org.w3c.dom.Node xmlNode, String outputName, ExtensibleXmlParser parser) {
        Map<String, ItemDefinition> itemDefinitions = (Map<String, ItemDefinition>) ((ProcessBuildData) parser.getData())
                .getMetaData("ItemDefinitions");
        String itemSubjectRef = ((Element) xmlNode).getAttribute("itemSubjectRef");

        if (itemSubjectRef == null || itemSubjectRef.isEmpty()) {
            String dataType = ((Element) xmlNode).getAttribute("dtype");
            if (dataType == null || dataType.isEmpty()) {
                dataType = "java.lang.String";
            }
            dataOutputTypes.put(outputName, dataType);
        } else if (itemDefinitions.get(itemSubjectRef) != null) {
            dataOutputTypes.put(outputName, itemDefinitions.get(itemSubjectRef).getStructureRef());
        } else {
            dataOutputTypes.put(outputName, "java.lang.Object");
        }
    }

    public void writeNode(Node node, StringBuilder xmlDump, int metaDataType) {
        throw new IllegalArgumentException("Writing out should be handled by specific handlers");
    }

}
