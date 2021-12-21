package io.automatiko.engine.workflow.expression;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.mvel2.integration.VariableResolverFactory;

import com.fasterxml.jackson.databind.JsonNode;

import io.automatiko.engine.api.expression.ExpressionEvaluator;
import io.automatiko.engine.workflow.base.core.Context;
import io.automatiko.engine.workflow.base.core.context.AbstractContext;
import io.automatiko.engine.workflow.process.core.WorkflowProcess;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public class JqExpressionEvaluator extends AbstractContext implements ExpressionEvaluator<VariableResolverFactory> {

    private static final long serialVersionUID = 2252395199600357250L;

    private Map<String, JsonQuery> cache = new ConcurrentHashMap<String, JsonQuery>();

    private Scope rootScope;

    public JqExpressionEvaluator(io.automatiko.engine.api.definition.process.Process process) {

        Set<String> imports = ((WorkflowProcess) process).getImports();

        addImports(imports);
    }

    @Override
    public String getType() {
        return EXPRESSION_EVALUATOR;
    }

    @Override
    public Context resolveContext(Object param) {
        return this;
    }

    @Override
    public Object evaluate(String expression, Map<String, Object> variables) {
        return evaluate(expression, (JsonNode) variables.get("workflowdata"),
                (Map<String, JsonNode>) variables.get("workflow_variables"));
    }

    @Override
    public Object evaluate(String expression, VariableResolverFactory resolver) {
        JsonNode workflowdata = (JsonNode) resolver.getVariableResolver("workflowdata").getValue();
        return evaluate(expression, workflowdata,
                null);
    }

    @Override
    public void addImports(Collection<String> imports) {
        rootScope = Scope.newEmptyScope();
        rootScope.loadFunctions(Scope.class.getClassLoader());
    }

    protected Object evaluate(String expression, JsonNode data, Map<String, JsonNode> variables) {
        JsonQuery compiled = cache.computeIfAbsent(expression, k -> {
            try {
                return JsonQuery.compile(expression);
            } catch (JsonQueryException e) {
                throw new RuntimeException("Error compiling JQ expression", e);
            }
        });

        Scope childScope = Scope.newChildScope(rootScope);
        if (variables != null) {
            for (Entry<String, JsonNode> entry : variables.entrySet()) {
                childScope.setValue(entry.getKey(), entry.getValue());
            }
        }
        try {
            List<JsonNode> out = compiled.apply(childScope, data);
            if (out.isEmpty()) {
                return null;
            }
            return out.get(0);
        } catch (JsonQueryException e) {
            throw new RuntimeException("Error evaluting JQ expression", e);
        }

    }
}
