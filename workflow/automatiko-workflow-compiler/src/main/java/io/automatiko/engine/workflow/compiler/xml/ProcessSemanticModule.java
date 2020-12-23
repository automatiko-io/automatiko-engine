
package io.automatiko.engine.workflow.compiler.xml;

import io.automatiko.engine.workflow.compiler.xml.processes.ActionNodeHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.CompositeNodeHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.ConnectionHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.ConstraintHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.DynamicNodeHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.EndNodeHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.EventFilterHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.EventNodeHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.ExceptionHandlerHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.FaultNodeHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.ForEachNodeHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.FunctionImportHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.GlobalHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.HumanTaskNodeHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.ImportHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.InPortHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.JoinNodeHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.MappingHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.MetaDataHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.MilestoneNodeHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.OutPortHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.ParameterHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.ProcessHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.RuleSetNodeHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.SplitNodeHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.StartNodeHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.StateNodeHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.SubProcessNodeHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.SwimlaneHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.TimerHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.TimerNodeHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.TriggerHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.TypeHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.ValueHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.VariableHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.WorkHandler;
import io.automatiko.engine.workflow.compiler.xml.processes.WorkItemNodeHandler;

public class ProcessSemanticModule extends DefaultSemanticModule implements SemanticModule {

	public static final String URI = "http://drools.org/drools-5.0/process";

	public ProcessSemanticModule() {
		super(URI);

		addHandler("process", new ProcessHandler());
		addHandler("start", new StartNodeHandler());
		addHandler("end", new EndNodeHandler());
		addHandler("actionNode", new ActionNodeHandler());
		addHandler("ruleSet", new RuleSetNodeHandler());
		addHandler("subProcess", new SubProcessNodeHandler());
		addHandler("workItem", new WorkItemNodeHandler());
		addHandler("split", new SplitNodeHandler());
		addHandler("join", new JoinNodeHandler());
		addHandler("milestone", new MilestoneNodeHandler());
		addHandler("timerNode", new TimerNodeHandler());
		addHandler("humanTask", new HumanTaskNodeHandler());
		addHandler("forEach", new ForEachNodeHandler());
		addHandler("composite", new CompositeNodeHandler());
		addHandler("connection", new ConnectionHandler());
		addHandler("import", new ImportHandler());
		addHandler("functionImport", new FunctionImportHandler());
		addHandler("global", new GlobalHandler());
		addHandler("variable", new VariableHandler());
		addHandler("swimlane", new SwimlaneHandler());
		addHandler("type", new TypeHandler());
		addHandler("value", new ValueHandler());
		addHandler("work", new WorkHandler());
		addHandler("parameter", new ParameterHandler());
		addHandler("mapping", new MappingHandler());
		addHandler("constraint", new ConstraintHandler());
		addHandler("in-port", new InPortHandler());
		addHandler("out-port", new OutPortHandler());
		addHandler("eventNode", new EventNodeHandler());
		addHandler("eventFilter", new EventFilterHandler());
		addHandler("fault", new FaultNodeHandler());
		addHandler("exceptionHandler", new ExceptionHandlerHandler());
		addHandler("timer", new TimerHandler());
		addHandler("trigger", new TriggerHandler());
		addHandler("state", new StateNodeHandler());
		addHandler("dynamic", new DynamicNodeHandler());
		addHandler("metaData", new MetaDataHandler());
	}
}
