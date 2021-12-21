
package io.automatiko.engine.workflow.process.executable.core;

import io.automatiko.engine.workflow.base.core.context.exception.ExceptionScope;
import io.automatiko.engine.workflow.base.core.context.swimlane.SwimlaneContext;
import io.automatiko.engine.workflow.base.core.context.variable.JsonVariableScope;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.expression.JqExpressionEvaluator;

public class ServerlessExecutableProcess extends ExecutableProcess {

    private static final long serialVersionUID = 511l;

    public ServerlessExecutableProcess() {
        setType(WORKFLOW_TYPE);
        VariableScope variableScope = new JsonVariableScope();
        addContext(variableScope);
        setDefaultContext(variableScope);
        SwimlaneContext swimLaneContext = new SwimlaneContext();
        addContext(swimLaneContext);
        setDefaultContext(swimLaneContext);
        ExceptionScope exceptionScope = new ExceptionScope();
        addContext(exceptionScope);
        setDefaultContext(exceptionScope);

        setDefaultContext(new JqExpressionEvaluator(this));
    }

}
