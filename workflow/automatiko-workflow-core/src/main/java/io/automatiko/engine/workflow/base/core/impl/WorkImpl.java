
package io.automatiko.engine.workflow.base.core.impl;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import io.automatiko.engine.workflow.base.core.ParameterDefinition;
import io.automatiko.engine.workflow.base.core.Work;

public class WorkImpl implements Work, Serializable {

    private static final long serialVersionUID = 510l;

    private String name;
    private Map<String, Object> parameters = new LinkedHashMap<String, Object>();
    private Map<String, ParameterDefinition> parameterDefinitions = new LinkedHashMap<String, ParameterDefinition>();

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setParameter(String name, Object value) {
        if (name == null) {
            throw new NullPointerException("Parameter name is null");
        }
        parameters.put(name, value);
    }

    public void setParameters(Map<String, Object> parameters) {
        if (parameters == null) {
            throw new NullPointerException();
        }
        this.parameters = new HashMap<String, Object>(parameters);
    }

    public Object getParameter(String name) {
        if (name == null) {
            throw new NullPointerException("Parameter name is null");
        }
        return parameters.get(name);
    }

    public Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public String toString() {
        return "Work " + name;
    }

    public void setParameterDefinitions(Set<ParameterDefinition> parameterDefinitions) {
        this.parameterDefinitions.clear();
        for (ParameterDefinition parameterDefinition : parameterDefinitions) {
            addParameterDefinition(parameterDefinition);
        }
    }

    public void addParameterDefinition(ParameterDefinition parameterDefinition) {
        this.parameterDefinitions.put(parameterDefinition.getName(), parameterDefinition);
    }

    public Set<ParameterDefinition> getParameterDefinitions() {
        return new LinkedHashSet<ParameterDefinition>(parameterDefinitions.values());
    }

    public String[] getParameterNames() {
        return parameterDefinitions.keySet().toArray(new String[parameterDefinitions.size()]);
    }

    public ParameterDefinition getParameterDefinition(String name) {
        return parameterDefinitions.get(name);
    }

    public ParameterDefinition removeParameterDefinition(String name) {
        return parameterDefinitions.remove(name);
    }

}
