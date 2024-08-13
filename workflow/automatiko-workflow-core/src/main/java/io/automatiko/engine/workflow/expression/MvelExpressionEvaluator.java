package io.automatiko.engine.workflow.expression;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.mvel2.MVEL;
import org.mvel2.ParserConfiguration;
import org.mvel2.ParserContext;
import org.mvel2.integration.VariableResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.expression.ExpressionEvaluator;
import io.automatiko.engine.workflow.base.core.Context;
import io.automatiko.engine.workflow.base.core.context.AbstractContext;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.core.datatype.impl.type.ObjectDataType;
import io.automatiko.engine.workflow.process.core.WorkflowProcess;

public class MvelExpressionEvaluator extends AbstractContext implements ExpressionEvaluator<VariableResolverFactory> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MvelExpressionEvaluator.class);

    ParserConfiguration configuration = new ParserConfiguration();

    private ParserContext context;

    private Map<String, Serializable> cache = new ConcurrentHashMap<>();

    public MvelExpressionEvaluator(io.automatiko.engine.api.definition.process.Process process) {

        Set<String> imports = ((WorkflowProcess) process).getImports();
        VariableScope processVariableScope = (VariableScope) ((io.automatiko.engine.workflow.process.core.WorkflowProcess) process)
                .getDefaultContext(VariableScope.VARIABLE_SCOPE);

        if (processVariableScope != null && processVariableScope.getVariables() != null) {
            for (Variable var : processVariableScope.getVariables()) {
                if (var.getType() instanceof ObjectDataType && var.getType().getClassType() != null) {
                    Class<?> varType = var.getType().getClassType();

                    imports.add(varType.getCanonicalName());
                }
            }
        }
        addImports(imports);
    }

    @Override
    public void addImports(Collection<String> imports) {
        for (String i : imports) {
            try {
                Class<?> clazz = Class.forName(i, false, Thread.currentThread().getContextClassLoader());

                for (Method m : clazz.getMethods()) {

                    if (Modifier.isStatic(m.getModifiers())) {
                        configuration.addImport(m.getName(), m);
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Unable to add imports for expression evaluation due to {}", e.getMessage(), e);
            }
        }
        context = new ParserContext(configuration);

    }

    @Override
    public Object evaluate(String expression, VariableResolverFactory resolver) {
        Serializable compiled = cache.computeIfAbsent(expression, k -> MVEL.compileExpression(expression, context));

        return MVEL.executeExpression(compiled, resolver);
    }

    public Object evaluate(String expression, Map<String, Object> variables) {

        Serializable compiled = cache.computeIfAbsent(expression, k -> MVEL.compileExpression(expression, context));

        return MVEL.executeExpression(compiled, variables);
    }

    @Override
    public String getType() {
        return EXPRESSION_EVALUATOR;
    }

    @Override
    public Context resolveContext(Object param) {
        return this;
    }

}
