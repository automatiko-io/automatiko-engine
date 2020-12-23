
package io.automatiko.engine.workflow.process.core.node;

import java.util.HashSet;
import java.util.Set;

import io.automatiko.engine.workflow.base.core.ParameterDefinition;
import io.automatiko.engine.workflow.base.core.Work;
import io.automatiko.engine.workflow.base.core.datatype.impl.type.StringDataType;
import io.automatiko.engine.workflow.base.core.impl.ParameterDefinitionImpl;
import io.automatiko.engine.workflow.base.core.impl.WorkImpl;

public class HumanTaskNode extends WorkItemNode {

	private static final long serialVersionUID = 510l;

	private String swimlane;

	public HumanTaskNode() {
		Work work = new WorkImpl();
		work.setName("Human Task");
		Set<ParameterDefinition> parameterDefinitions = new HashSet<ParameterDefinition>();
		parameterDefinitions.add(new ParameterDefinitionImpl("TaskName", new StringDataType()));
		parameterDefinitions.add(new ParameterDefinitionImpl("ActorId", new StringDataType()));
		parameterDefinitions.add(new ParameterDefinitionImpl("Priority", new StringDataType()));
		parameterDefinitions.add(new ParameterDefinitionImpl("Comment", new StringDataType()));
		parameterDefinitions.add(new ParameterDefinitionImpl("Skippable", new StringDataType()));
		parameterDefinitions.add(new ParameterDefinitionImpl("Content", new StringDataType()));
		// TODO: initiator
		// TODO: attachments
		// TODO: deadlines
		// TODO: delegates
		// TODO: recipients
		// TODO: ...
		work.setParameterDefinitions(parameterDefinitions);
		setWork(work);
	}

	public String getSwimlane() {
		return swimlane;
	}

	public void setSwimlane(String swimlane) {
		this.swimlane = swimlane;
	}

}
