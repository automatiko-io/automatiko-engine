
package io.automatiko.engine.workflow.base.instance.context.variable;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.automatiko.engine.workflow.base.core.context.variable.JsonVariableScope;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;

/**
 * 
 */
public class JsonVariableScopeInstance extends VariableScopeInstance {

    private static final long serialVersionUID = 510l;

    private ObjectNode variables = new ObjectNode(JsonNodeFactory.withExactBigDecimals(false));

    public String getContextType() {
        return VariableScope.VARIABLE_SCOPE;
    }

    public Map<String, Object> getVariables() {
        Map<String, Object> copy = new LinkedHashMap<>();
        Iterator<Entry<String, JsonNode>> it = variables.fields();

        while (it.hasNext()) {
            Entry<String, JsonNode> entry = (Entry<String, JsonNode>) it.next();
            copy.put(entry.getKey(), entry.getValue());
        }

        return copy;
    }

    public void internalSetVariable(String name, Object value) {
        if (name.equals(JsonVariableScope.WORKFLOWDATA_KEY)) {
            this.variables = (ObjectNode) value;
            return;
        }
        this.variables.set(name, (JsonNode) value);
    }

    public Object internalGetVariable(String name) {
        if (name.equals(JsonVariableScope.WORKFLOWDATA_KEY)) {
            return this.variables;
        }
        return this.variables.get(name);
    }

}
