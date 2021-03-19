
package io.automatiko.engine.workflow.bpmn2.xml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import io.automatiko.engine.api.runtime.process.DataTransformer;
import io.automatiko.engine.workflow.base.core.event.EventFilter;
import io.automatiko.engine.workflow.base.core.event.EventTransformerImpl;
import io.automatiko.engine.workflow.base.core.event.EventTypeFilter;
import io.automatiko.engine.workflow.base.core.event.NonAcceptingEventTypeFilter;
import io.automatiko.engine.workflow.base.core.impl.DataTransformerRegistry;
import io.automatiko.engine.workflow.base.core.timer.DateTimeUtils;
import io.automatiko.engine.workflow.base.core.timer.Timer;
import io.automatiko.engine.workflow.bpmn2.core.Error;
import io.automatiko.engine.workflow.bpmn2.core.Escalation;
import io.automatiko.engine.workflow.bpmn2.core.ItemDefinition;
import io.automatiko.engine.workflow.bpmn2.core.Message;
import io.automatiko.engine.workflow.bpmn2.core.Signal;
import io.automatiko.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatiko.engine.workflow.compiler.xml.ProcessBuildData;
import io.automatiko.engine.workflow.compiler.xml.XmlDumper;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.impl.ConsequenceAction;
import io.automatiko.engine.workflow.process.core.node.ConstraintTrigger;
import io.automatiko.engine.workflow.process.core.node.EventSubProcessNode;
import io.automatiko.engine.workflow.process.core.node.EventTrigger;
import io.automatiko.engine.workflow.process.core.node.StartNode;
import io.automatiko.engine.workflow.process.core.node.Transformation;
import io.automatiko.engine.workflow.process.core.node.Trigger;

public class StartEventHandler extends AbstractNodeHandler {

    private static final String TRIGGER_REF = "TriggerRef";
    private static final String MESSAGE_TYPE = "MessageType";
    private static final String TRIGGER_TYPE = "TriggerType";
    private static final String TRIGGER_CORRELATION = "TriggerCorrelation";
    private static final String TRIGGER_CORRELATION_EXPR = "TriggerCorrelationExpr";
    private static final String TRIGGER_TOPIC_EXPR = "TriggerTopicExpr";

    protected DataTransformerRegistry transformerRegistry = DataTransformerRegistry.get();

    @Override
    protected Node createNode(Attributes attrs) {
        return new StartNode();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class generateNodeFor() {
        return StartNode.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void handleNode(final Node node, final Element element, final String uri, final String localName,
            final ExtensibleXmlParser parser) throws SAXException {
        super.handleNode(node, element, uri, localName, parser);
        StartNode startNode = (StartNode) node;
        // TODO: StartEventHandler.handleNode(): the parser doesn't discriminate between
        // the schema default and the actual set value
        // However, while the schema says the "isInterrupting" attr should default to
        // true
        // The spec says that Escalation start events should default to not
        // interrupting..
        startNode.setInterrupting(Boolean.parseBoolean(element.getAttribute("isInterrupting")));

        org.w3c.dom.Node xmlNode = element.getFirstChild();
        while (xmlNode != null) {
            String nodeName = xmlNode.getNodeName();
            if ("dataOutput".equals(nodeName)) {
                readDataOutput(xmlNode, parser);
            } else if ("dataOutputAssociation".equals(nodeName)) {
                readDataOutputAssociation(xmlNode, startNode);
            } else if ("outputSet".equals(nodeName)) {
                // p. 225, BPMN2 spec (2011-01-03)
                // InputSet and OutputSet elements imply that process execution should wait for
                // them to be filled
                // and are therefore not applicable to catch events
                String message = "Ignoring <" + nodeName + "> element: " + "<" + nodeName
                        + "> elements should not be used on start or other catch events.";
                SAXParseException saxpe = new SAXParseException(message, parser.getLocator());
                parser.warning(saxpe);
                // no exception thrown for backwards compatibility (we used to ignore these
                // elements)
            } else if ("conditionalEventDefinition".equals(nodeName)) {
                String constraint = null;
                org.w3c.dom.Node subNode = xmlNode.getFirstChild();
                while (subNode != null) {
                    String subnodeName = subNode.getNodeName();
                    if ("condition".equals(subnodeName)) {
                        constraint = xmlNode.getTextContent();
                        break;
                    }
                    subNode = subNode.getNextSibling();
                }
                ConstraintTrigger trigger = new ConstraintTrigger();
                trigger.setConstraint(constraint);
                startNode.addTrigger(trigger);
                startNode.setMetaData(TRIGGER_REF, "Condition-" + node.getId());
                startNode.setMetaData(TRIGGER_TYPE, "Condition");
                break;
            } else if ("signalEventDefinition".equals(nodeName)) {
                String type = ((Element) xmlNode).getAttribute("signalRef");

                type = checkSignalAndConvertToRealSignalNam(parser, type);

                if (type != null && type.trim().length() > 0) {
                    addTriggerWithInMappings(startNode, type);
                }
                startNode.setMetaData(MESSAGE_TYPE, type);
                startNode.setMetaData(TRIGGER_TYPE, "Signal");
                Signal signal = findSignalByName(parser, type);
                if (signal != null) {
                    String eventType = signal.getStructureRef();
                    startNode.setMetaData(TRIGGER_REF, eventType);
                } else {
                    startNode.setMetaData(TRIGGER_REF, type);
                }
            } else if ("messageEventDefinition".equals(nodeName)) {
                String messageRef = ((Element) xmlNode).getAttribute("messageRef");
                Map<String, Message> messages = (Map<String, Message>) ((ProcessBuildData) parser.getData())
                        .getMetaData("Messages");
                if (messages == null) {
                    throw new IllegalArgumentException("No messages found");
                }
                Message message = messages.get(messageRef);
                if (message == null) {
                    throw new IllegalArgumentException("Could not find message " + messageRef);
                }
                startNode.setMetaData(MESSAGE_TYPE, message.getType());
                startNode.setMetaData(TRIGGER_TYPE, "ConsumeMessage");
                startNode.setMetaData(TRIGGER_REF, message.getName());
                startNode.setMetaData(TRIGGER_CORRELATION, message.getCorrelation());
                startNode.setMetaData(TRIGGER_CORRELATION_EXPR, message.getCorrelationExpression());
                startNode.setMetaData("connector",
                        message.getMetaData().getOrDefault("connector", startNode.getMetaData("connector")));
                startNode.setMetaData("topic", message.getMetaData().getOrDefault("topic", startNode.getMetaData("topic")));

                addTriggerWithInMappings(startNode, "Message-" + message.getName());
            } else if ("timerEventDefinition".equals(nodeName)) {
                handleTimerNode(startNode, element, uri, localName, parser);
                startNode.setMetaData(TRIGGER_TYPE, "Timer");
                // following event definitions are only for event sub process and will be
                // validated to not be included in top process definitions
            } else if ("errorEventDefinition".equals(nodeName)) {
                if (!startNode.isInterrupting()) {
                    // BPMN2 spec (p.245-246, (2011-01-03)) implies that
                    // - a <startEvent> in an Event Sub-Process
                    // - *without* the 'isInterupting' attribute always interrupts (containing
                    // process)
                    String errorMsg = "Error Start Events in an Event Sub-Process always interrupt the containing (sub)process(es).";
                    throw new IllegalArgumentException(errorMsg);
                }
                String errorRef = ((Element) xmlNode).getAttribute("errorRef");
                if (errorRef != null && errorRef.trim().length() > 0) {
                    List<Error> errors = (List<Error>) ((ProcessBuildData) parser.getData()).getMetaData("Errors");
                    if (errors == null) {
                        throw new IllegalArgumentException("No errors found");
                    }
                    Error error = null;
                    for (Error listError : errors) {
                        if (errorRef.equals(listError.getId())) {
                            error = listError;
                        }
                    }
                    if (error == null) {
                        throw new IllegalArgumentException("Could not find error " + errorRef);
                    }
                    startNode.setMetaData("FaultCode", error.getErrorCode());
                    addTriggerWithInMappings(startNode, "Error-" + error.getErrorCode());

                    startNode.setMetaData(TRIGGER_TYPE, "Error");
                    startNode.setMetaData(TRIGGER_REF, "Error-" + error.getErrorCode());
                    if (error.getMetaData().get("retry") != null) {
                        startNode.setMetaData("ErrorRetry",
                                ((Long) DateTimeUtils.parseDuration((String) error.getMetaData().get("retry"))).intValue());
                        if (error.getMetaData().get("retryLimit") != null) {
                            startNode.setMetaData("ErrorRetryLimit",
                                    Integer.parseInt((String) error.getMetaData().get("retryLimit")));
                        }
                    }
                }
            } else if ("escalationEventDefinition".equals(nodeName)) {
                String escalationRef = ((Element) xmlNode).getAttribute("escalationRef");
                if (escalationRef != null && escalationRef.trim().length() > 0) {
                    Map<String, Escalation> escalations = (Map<String, Escalation>) ((ProcessBuildData) parser
                            .getData()).getMetaData(ProcessHandler.ESCALATIONS);
                    if (escalations == null) {
                        throw new IllegalArgumentException("No escalations found");
                    }
                    Escalation escalation = escalations.get(escalationRef);
                    if (escalation == null) {
                        throw new IllegalArgumentException("Could not find escalation " + escalationRef);
                    }

                    addTriggerWithInMappings(startNode, "Escalation-" + escalation.getEscalationCode());
                    startNode.setMetaData(TRIGGER_TYPE, "Escalation");
                }
            } else if ("compensateEventDefinition".equals(nodeName)) {
                handleCompensationNode(startNode, xmlNode);
            }
            xmlNode = xmlNode.getNextSibling();
        }

        node.setMetaData("DataOutputs", new LinkedHashMap<String, String>(dataOutputTypes));
    }

    private void addTriggerWithInMappings(StartNode startNode, String triggerEventType) {
        EventTrigger trigger = new EventTrigger();
        EventTypeFilter eventFilter = new EventTypeFilter();
        eventFilter.setType(triggerEventType);
        trigger.addEventFilter(eventFilter);

        String mapping = (String) startNode.getMetaData("TriggerMapping");
        if (mapping != null) {
            trigger.addInMapping(mapping, startNode.getOutMapping(mapping));
        }

        startNode.addTrigger(trigger);
    }

    protected void readDataOutputAssociation(org.w3c.dom.Node xmlNode, StartNode startNode) {
        // sourceRef
        org.w3c.dom.Node subNode = xmlNode.getFirstChild();
        if (!"sourceRef".equals(subNode.getNodeName())) {
            throw new IllegalArgumentException("No sourceRef found in dataOutputAssociation in startEvent");
        }
        String source = subNode.getTextContent();
        if (dataOutputs.get(source) == null) {
            throw new IllegalArgumentException("No dataOutput could be found for the dataOutputAssociation.");
        }
        String target = null;
        Transformation transformation = null;
        // targetRef
        subNode = subNode.getNextSibling();
        if (subNode != null && "targetRef".equals(subNode.getNodeName())) {
            target = subNode.getTextContent();
            if (target != null) {
                startNode.setMetaData("TriggerMapping", target);
            }
            // transformation

            subNode = subNode.getNextSibling();
        }
        if (subNode != null && "transformation".equals(subNode.getNodeName())) {
            String lang = subNode.getAttributes().getNamedItem("language").getNodeValue();
            String expression = subNode.getTextContent();
            DataTransformer transformer = transformerRegistry.find(lang);
            if (transformer == null) {
                throw new IllegalArgumentException("No transformer registered for language " + lang);
            }
            transformation = new Transformation(lang, expression, dataOutputs.get(source));
            startNode.setMetaData("Transformation", transformation);

            startNode.setEventTransformer(new EventTransformerImpl(transformation));
            subNode = subNode.getNextSibling();

            startNode.addOutMapping(target, dataOutputs.get(source));
        } else if (subNode != null && "assignment".equals(subNode.getNodeName())) {

            // assignments
            while (subNode != null) {
                org.w3c.dom.Node ssubNode = subNode.getFirstChild();
                target = ssubNode.getNextSibling().getTextContent();
                subNode = subNode.getNextSibling();

            }
            startNode.setMetaData("TriggerMapping", target);

            startNode.addOutMapping(dataOutputs.get(source), target);
        } else {
            startNode.addOutMapping(dataOutputs.get(source), target);
        }

    }

    // The results of this method are only used to check syntax
    protected void readDataOutput(org.w3c.dom.Node xmlNode, ExtensibleXmlParser parser) {
        String id = ((Element) xmlNode).getAttribute("id");
        String outputName = ((Element) xmlNode).getAttribute("name");
        dataOutputs.put(id, outputName);
        populateDataOutputs(xmlNode, outputName, parser);
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

    @Override
    public void writeNode(Node node, StringBuilder xmlDump, int metaDataType) {
        StartNode startNode = (StartNode) node;
        writeNode("startEvent", startNode, xmlDump, metaDataType);
        xmlDump.append(" isInterrupting=\"");
        if (startNode.isInterrupting()) {
            xmlDump.append("true");
        } else {
            xmlDump.append("false");
        }
        xmlDump.append("\">" + EOL);
        writeExtensionElements(startNode, xmlDump);

        List<Trigger> triggers = startNode.getTriggers();
        if (triggers != null) {
            if (triggers.size() > 1) {
                throw new IllegalArgumentException("Multiple start triggers not supported");
            }

            Trigger trigger = triggers.get(0);
            if (trigger instanceof ConstraintTrigger) {
                ConstraintTrigger constraintTrigger = (ConstraintTrigger) trigger;
                if (constraintTrigger.getHeader() == null) {
                    xmlDump.append("      <conditionalEventDefinition>" + EOL);
                    xmlDump.append("        <condition xsi:type=\"tFormalExpression\" language=\""
                            + XmlBPMNProcessDumper.RULE_LANGUAGE + "\">" + constraintTrigger.getConstraint()
                            + "</condition>" + EOL);
                    xmlDump.append("      </conditionalEventDefinition>" + EOL);
                }
            } else if (trigger instanceof EventTrigger) {
                EventTrigger eventTrigger = (EventTrigger) trigger;
                String mapping = null;
                String nameMapping = "event";
                if (!trigger.getInMappings().isEmpty()) {
                    mapping = eventTrigger.getInMappings().keySet().iterator().next();
                    nameMapping = eventTrigger.getInMappings().values().iterator().next();
                } else {
                    mapping = (String) startNode.getMetaData("TriggerMapping");
                }

                if (mapping != null) {
                    xmlDump.append("      <dataOutput id=\"_" + startNode.getId() + "_Output\" name=\"" + nameMapping
                            + "\" />" + EOL + "      <dataOutputAssociation>" + EOL + "        <sourceRef>_"
                            + startNode.getId() + "_Output</sourceRef>" + EOL + "        <targetRef>" + mapping
                            + "</targetRef>" + EOL + "      </dataOutputAssociation>" + EOL);
                }

                String type = ((EventTypeFilter) eventTrigger.getEventFilters().get(0)).getType();
                if (type.startsWith("Message-")) {
                    type = type.substring(8);
                    xmlDump.append("      <messageEventDefinition messageRef=\"" + type + "\"/>" + EOL);
                } else if (type.startsWith("Error-")) {
                    type = type.substring(6);
                    String errorId = getErrorIdForErrorCode(type, startNode);
                    xmlDump.append("      <errorEventDefinition errorRef=\""
                            + XmlBPMNProcessDumper.replaceIllegalCharsAttribute(errorId) + "\"/>" + EOL);
                } else if (type.startsWith("Escalation-")) {
                    type = type.substring(11);
                    xmlDump.append("      <escalationEventDefinition escalationRef=\"" + type + "\"/>" + EOL);
                } else if (type.equals("Compensation")) {
                    xmlDump.append("      <compensateEventDefinition/>" + EOL);
                } else {
                    xmlDump.append("      <signalEventDefinition signalRef=\"" + type + "\" />" + EOL);
                }
            } else {
                throw new IllegalArgumentException("Unsupported trigger type " + trigger);
            }

            if (startNode.getTimer() != null) {
                Timer timer = startNode.getTimer();
                xmlDump.append("      <timerEventDefinition>" + EOL);
                if (timer != null && (timer.getDelay() != null || timer.getDate() != null)) {
                    if (timer.getTimeType() == Timer.TIME_DATE) {
                        xmlDump.append("        <timeDate xsi:type=\"tFormalExpression\">"
                                + XmlDumper.replaceIllegalChars(timer.getDate()) + "</timeDate>" + EOL);
                    } else if (timer.getTimeType() == Timer.TIME_DURATION) {
                        xmlDump.append("        <timeDuration xsi:type=\"tFormalExpression\">"
                                + XmlDumper.replaceIllegalChars(timer.getDelay()) + "</timeDuration>" + EOL);
                    } else if (timer.getTimeType() == Timer.TIME_CYCLE) {

                        if (timer.getPeriod() != null) {
                            xmlDump.append("        <timeCycle xsi:type=\"tFormalExpression\">"
                                    + XmlDumper.replaceIllegalChars(timer.getDelay()) + "###"
                                    + XmlDumper.replaceIllegalChars(timer.getPeriod()) + "</timeCycle>" + EOL);
                        } else {
                            xmlDump.append("        <timeCycle xsi:type=\"tFormalExpression\">"
                                    + XmlDumper.replaceIllegalChars(timer.getDelay()) + "</timeCycle>" + EOL);
                        }
                    }
                }
                xmlDump.append("      </timerEventDefinition>" + EOL);
            }
        } else if (startNode.getTimer() != null) {
            Timer timer = startNode.getTimer();
            xmlDump.append("      <timerEventDefinition>" + EOL);
            if (timer != null && (timer.getDelay() != null || timer.getDate() != null)) {
                if (timer.getTimeType() == Timer.TIME_DATE) {
                    xmlDump.append("        <timeDate xsi:type=\"tFormalExpression\">"
                            + XmlDumper.replaceIllegalChars(timer.getDate()) + "</timeDate>" + EOL);
                } else if (timer.getTimeType() == Timer.TIME_DURATION) {
                    xmlDump.append("        <timeDuration xsi:type=\"tFormalExpression\">"
                            + XmlDumper.replaceIllegalChars(timer.getDelay()) + "</timeDuration>" + EOL);
                } else if (timer.getTimeType() == Timer.TIME_CYCLE) {

                    if (timer.getPeriod() != null) {
                        xmlDump.append("        <timeCycle xsi:type=\"tFormalExpression\">"
                                + XmlDumper.replaceIllegalChars(timer.getDelay()) + "###"
                                + XmlDumper.replaceIllegalChars(timer.getPeriod()) + "</timeCycle>" + EOL);
                    } else {
                        xmlDump.append("        <timeCycle xsi:type=\"tFormalExpression\">"
                                + XmlDumper.replaceIllegalChars(timer.getDelay()) + "</timeCycle>" + EOL);
                    }
                }
            }
            xmlDump.append("      </timerEventDefinition>" + EOL);
        }
        endNode("startEvent", xmlDump);
    }

    protected void handleTimerNode(final Node node, final Element element, final String uri, final String localName,
            final ExtensibleXmlParser parser) throws SAXException {
        super.handleNode(node, element, uri, localName, parser);
        StartNode startNode = (StartNode) node;
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
                        } else {
                            timer.setPeriod(delay);
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
                startNode.setTimer(timer);
                if (parser.getParent() instanceof EventSubProcessNode) {
                    // handle timer on start events like normal (non rule) timers for event sub
                    // process

                    EventTrigger trigger = new EventTrigger();
                    EventTypeFilter eventFilter = new EventTypeFilter();
                    eventFilter.setType("Timer-" + ((EventSubProcessNode) parser.getParent()).getId());
                    trigger.addEventFilter(eventFilter);
                    String mapping = (String) startNode.getMetaData("TriggerMapping");
                    if (mapping != null) {
                        trigger.addInMapping(mapping, "event");
                    }
                    startNode.addTrigger(trigger);
                    ((EventSubProcessNode) parser.getParent()).addTimer(timer, new ConsequenceAction("java", ""));
                }
            }
            xmlNode = xmlNode.getNextSibling();
        }
    }

    protected void handleCompensationNode(final StartNode startNode, final org.w3c.dom.Node xmlNode) {
        if (startNode.isInterrupting()) {
            logger.warn("Compensation Event Sub-Processes [" + startNode.getMetaData("UniqueId")
                    + "] may not be specified as interrupting:"
                    + " overriding attribute and setting to not-interrupting.");
        }
        startNode.setInterrupting(false);

        /**
         * From the BPMN2 spec, P.264: "For a Start Event: This Event "catches" the
         * compensation for an Event Sub-Process. No further information is required.
         * The Event Sub-Process will provide the id necessary to match the Compensation
         * Event with the Event that threw the compensation"
         *
         * In other words, the id of the Sub-Process containing this Event Sub-Process
         * is what should be used as the activityRef value in any Intermediate (throw)
         * or End compensation event that targets this particular Event Sub-Process.
         *
         * This is similar to the logic used for a Compensation Boundary Event: it's
         * signaled using the id of the activity to which the CBE is attached to.
         */
        String activityRef = ((Element) xmlNode).getAttribute("activityRef");
        if (activityRef != null && activityRef.length() > 0) {
            logger.warn("activityRef value [" + activityRef + "] on Start Event '" + startNode.getMetaData("UniqueId")
                    + "' ignored per the BPMN2 specification.");
        }

        // so that this node will get processed in ProcessHandler.postProcessNodes(...)
        EventTrigger startTrigger = new EventTrigger();
        EventFilter eventFilter = new NonAcceptingEventTypeFilter();
        ((NonAcceptingEventTypeFilter) eventFilter).setType("Compensation");
        startTrigger.addEventFilter(eventFilter);
        List<Trigger> startTriggers = new ArrayList<>();
        startTriggers.add(startTrigger);
        startNode.setTriggers(startTriggers);
        String mapping = (String) startNode.getMetaData("TriggerMapping");
        if (mapping != null) {
            startTrigger.addInMapping(mapping, startNode.getOutMapping(mapping));
        }
    }
}
