
package io.automatiko.engine.workflow.process.executable.core.factory;

import java.util.Arrays;
import java.util.Collections;

import io.automatiko.engine.workflow.base.core.context.variable.Mappable;
import io.automatiko.engine.workflow.base.instance.impl.jq.TaskInputJqAssignmentAction;
import io.automatiko.engine.workflow.base.instance.impl.jq.TaskOutputJqAssignmentAction;
import io.automatiko.engine.workflow.process.core.node.Assignment;
import io.automatiko.engine.workflow.process.core.node.DataAssociation;

public interface MappableNodeFactory {

    String METHOD_IN_MAPPING = "inMapping";
    String METHOD_OUT_MAPPING = "outMapping";
    String METHOD_IN_JQ_MAPPING = "inMappingWithJqAssignment";
    String METHOD_OUT_JQ_MAPPING = "outMappingWithJqAssignment";

    Mappable getMappableNode();

    default MappableNodeFactory inMapping(String parameterName, String variableName) {
        getMappableNode().addInMapping(parameterName, variableName);
        return this;
    }

    default MappableNodeFactory outMapping(String parameterName, String variableName) {
        getMappableNode().addOutMapping(parameterName, variableName);
        return this;
    }

    default MappableNodeFactory outMappingWithJqAssignment(String outputExpression, String scopeExpression) {
        Assignment outAssignment = new Assignment("jq", null, null);
        outAssignment.setMetaData("Action", new TaskOutputJqAssignmentAction(outputExpression, scopeExpression));
        getMappableNode().addOutAssociation(
                new DataAssociation(Collections.emptyList(), "", Arrays.asList(outAssignment), null));
        return this;
    }

    default MappableNodeFactory inMappingWithJqAssignment(String inputExpression, String... params) {
        Assignment assignment = new Assignment("jq", null, null);
        assignment.setMetaData("Action", new TaskInputJqAssignmentAction(inputExpression, params));
        getMappableNode().addInAssociation(
                new DataAssociation(Collections.emptyList(), "", Arrays.asList(assignment), null));
        return this;
    }

}
