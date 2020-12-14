
package io.automatik.engine.workflow.bpmn2.xml;

import io.automatik.engine.workflow.compiler.xml.DefaultSemanticModule;
import io.automatik.engine.workflow.compiler.xml.processes.ActionNodeHandler;
import io.automatik.engine.workflow.process.core.node.ActionNode;
import io.automatik.engine.workflow.process.core.node.CatchLinkNode;
import io.automatik.engine.workflow.process.core.node.CompositeContextNode;
import io.automatik.engine.workflow.process.core.node.EndNode;
import io.automatik.engine.workflow.process.core.node.EventNode;
import io.automatik.engine.workflow.process.core.node.FaultNode;
import io.automatik.engine.workflow.process.core.node.ForEachNode;
import io.automatik.engine.workflow.process.core.node.Join;
import io.automatik.engine.workflow.process.core.node.Split;
import io.automatik.engine.workflow.process.core.node.StateNode;
import io.automatik.engine.workflow.process.core.node.ThrowLinkNode;
import io.automatik.engine.workflow.process.core.node.TimerNode;
import io.automatik.engine.workflow.process.core.node.WorkItemNode;

public class BPMNSemanticModule extends DefaultSemanticModule {

    public static final String BPMN2_URI = "http://www.omg.org/spec/BPMN/20100524/MODEL";

    public BPMNSemanticModule() {
        super(BPMN2_URI);

        addHandler("definitions", new DefinitionsHandler());
        addHandler("import", new Bpmn2ImportHandler());
        addHandler("process", new ProcessHandler());

        addHandler("property", new PropertyHandler());
        addHandler("lane", new LaneHandler());

        addHandler("startEvent", new StartEventHandler());
        addHandler("endEvent", new EndEventHandler());
        addHandler("exclusiveGateway", new ExclusiveGatewayHandler());
        addHandler("inclusiveGateway", new InclusiveGatewayHandler());
        addHandler("parallelGateway", new ParallelGatewayHandler());
        addHandler("eventBasedGateway", new EventBasedGatewayHandler());
        addHandler("complexGateway", new ComplexGatewayHandler());
        addHandler("scriptTask", new ScriptTaskHandler());
        addHandler("task", new TaskHandler());
        addHandler("userTask", new UserTaskHandler());
        addHandler("manualTask", new ManualTaskHandler());
        addHandler("serviceTask", new ServiceTaskHandler());
        addHandler("sendTask", new SendTaskHandler());
        addHandler("receiveTask", new ReceiveTaskHandler());
        addHandler("businessRuleTask", new BusinessRuleTaskHandler());
        addHandler("callActivity", new CallActivityHandler());
        addHandler("subProcess", new SubProcessHandler());
        addHandler("adHocSubProcess", new AdHocSubProcessHandler());
        addHandler("intermediateThrowEvent", new IntermediateThrowEventHandler());
        addHandler("intermediateCatchEvent", new IntermediateCatchEventHandler());
        addHandler("boundaryEvent", new BoundaryEventHandler());
        addHandler("dataObject", new DataObjectHandler());
        addHandler("dataInput", new DataObjectHandler());
        addHandler("dataOutput", new DataObjectHandler());
        addHandler("transaction", new TransactionHandler());

        addHandler("sequenceFlow", new SequenceFlowHandler());

        addHandler("itemDefinition", new ItemDefinitionHandler());
        addHandler("message", new MessageHandler());
        addHandler("signal", new SignalHandler());
        addHandler("interface", new InterfaceHandler());
        addHandler("operation", new OperationHandler());
        addHandler("inMessageRef", new InMessageRefHandler());
        addHandler("escalation", new EscalationHandler());
        addHandler("error", new ErrorHandler());
        addHandler("dataStore", new DataStoreHandler());
        addHandler("association", new AssociationHandler());
        addHandler("documentation", new DocumentationHandler());
        addHandler("resource", new ResourceHandler());

        handlersByClass.put(Split.class, new SplitHandler());
        handlersByClass.put(Join.class, new JoinHandler());
        handlersByClass.put(EventNode.class, new EventNodeHandler());
        handlersByClass.put(TimerNode.class, new TimerNodeHandler());
        handlersByClass.put(EndNode.class, new EndNodeHandler());
        handlersByClass.put(FaultNode.class, new FaultNodeHandler());
        handlersByClass.put(WorkItemNode.class, new WorkItemNodeHandler());
        handlersByClass.put(ActionNode.class, new ActionNodeHandler());
        handlersByClass.put(StateNode.class, new StateNodeHandler());
        handlersByClass.put(CompositeContextNode.class, new CompositeContextNodeHandler());
        handlersByClass.put(ForEachNode.class, new ForEachNodeHandler());
        handlersByClass.put(ThrowLinkNode.class, new ThrowLinkNodeHandler());
        handlersByClass.put(CatchLinkNode.class, new CatchLinkNodeHandler());

    }

}
