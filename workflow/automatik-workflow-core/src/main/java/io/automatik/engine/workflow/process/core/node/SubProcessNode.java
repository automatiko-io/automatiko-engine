
package io.automatik.engine.workflow.process.core.node;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.mvel2.MVEL;

import io.automatik.engine.api.Model;
import io.automatik.engine.api.definition.process.Connection;
import io.automatik.engine.api.runtime.process.DataTransformer;
import io.automatik.engine.api.runtime.process.ProcessContext;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.workflow.base.core.Context;
import io.automatik.engine.workflow.base.core.ContextContainer;
import io.automatik.engine.workflow.base.core.context.AbstractContext;
import io.automatik.engine.workflow.base.core.context.variable.Mappable;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.base.core.impl.ContextContainerImpl;
import io.automatik.engine.workflow.base.core.impl.DataTransformerRegistry;
import io.automatik.engine.workflow.base.instance.context.variable.VariableScopeInstance;
import io.automatik.engine.workflow.base.instance.impl.util.VariableUtil;
import io.automatik.engine.workflow.process.instance.NodeInstance;
import io.automatik.engine.workflow.process.instance.impl.NodeInstanceResolverFactory;
import io.automatik.engine.workflow.util.PatternConstants;

/**
 * Default implementation of a sub-flow node.
 * 
 */
public class SubProcessNode extends StateBasedNode implements Mappable, ContextContainer {

    private static final long serialVersionUID = 510l;

    // NOTE: ContetxInstances are not persisted as current functionality (exception
    // scope) does not require it
    private ContextContainer contextContainer = new ContextContainerImpl();

    private String processId;
    private String processVersion;
    private String processName;
    private boolean waitForCompletion = true;

    private List<DataAssociation> inMapping = new LinkedList<DataAssociation>();
    private List<DataAssociation> outMapping = new LinkedList<DataAssociation>();

    private boolean independent = false;
    private SubProcessFactory subProcessFactory;

    public void setProcessId(final String processId) {
        this.processId = processId;
    }

    public String getProcessId() {
        return this.processId;
    }

    public String getProcessVersion() {
        return processVersion;
    }

    public void setProcessVersion(String processVersion) {
        this.processVersion = processVersion;
    }

    public boolean isWaitForCompletion() {
        return waitForCompletion;
    }

    public void setWaitForCompletion(boolean waitForCompletion) {
        this.waitForCompletion = waitForCompletion;
    }

    public void addInMapping(String parameterName, String variableName) {
        inMapping.add(new DataAssociation(variableName, parameterName, null, null));
    }

    public void addInMapping(String parameterName, String variableName, Transformation transformation) {
        inMapping.add(new DataAssociation(variableName, parameterName, null, transformation));
    }

    public void setInMappings(Map<String, String> inMapping) {
        this.inMapping = new LinkedList<DataAssociation>();
        for (Map.Entry<String, String> entry : inMapping.entrySet()) {
            addInMapping(entry.getKey(), entry.getValue());
        }
    }

    public String getInMapping(String parameterName) {
        return getInMappings().get(parameterName);
    }

    public Map<String, String> getInMappings() {
        Map<String, String> in = new HashMap<String, String>();
        for (DataAssociation a : inMapping) {
            if (a.getSources().size() == 1 && (a.getAssignments() == null || a.getAssignments().size() == 0)
                    && a.getTransformation() == null) {
                in.put(a.getTarget(), a.getSources().get(0));
            }
        }
        return in;
    }

    public void addInAssociation(DataAssociation dataAssociation) {
        inMapping.add(dataAssociation);
    }

    public List<DataAssociation> getInAssociations() {
        return Collections.unmodifiableList(inMapping);
    }

    public void addOutMapping(String parameterName, String variableName) {
        outMapping.add(new DataAssociation(parameterName, variableName, null, null));
    }

    public void addOutMapping(String parameterName, String variableName, Transformation transformation) {
        outMapping.add(new DataAssociation(parameterName, variableName, null, transformation));
    }

    public void setOutMappings(Map<String, String> outMapping) {
        this.outMapping = new LinkedList<DataAssociation>();
        for (Map.Entry<String, String> entry : outMapping.entrySet()) {
            addOutMapping(entry.getKey(), entry.getValue());
        }
    }

    public String getOutMapping(String parameterName) {
        return getOutMappings().get(parameterName);
    }

    public Map<String, String> getOutMappings() {
        Map<String, String> out = new HashMap<String, String>();
        for (DataAssociation a : outMapping) {
            if (a.getSources().size() == 1 && (a.getAssignments() == null || a.getAssignments().size() == 0)
                    && a.getTransformation() == null) {
                out.put(a.getSources().get(0), a.getTarget());
            }
        }
        return out;
    }

    public void adjustOutMapping(String forEachOutVariable) {
        if (forEachOutVariable == null) {
            return;
        }
        Iterator<DataAssociation> it = outMapping.iterator();
        while (it.hasNext()) {
            DataAssociation association = it.next();
            if (forEachOutVariable.equals(association.getTarget())) {
                it.remove();
            }
        }
    }

    public void addOutAssociation(DataAssociation dataAssociation) {
        outMapping.add(dataAssociation);
    }

    public List<DataAssociation> getOutAssociations() {
        return Collections.unmodifiableList(outMapping);
    }

    public boolean isIndependent() {
        return independent;
    }

    public void setIndependent(boolean independent) {
        this.independent = independent;
    }

    public void validateAddIncomingConnection(final String type, final Connection connection) {
        super.validateAddIncomingConnection(type, connection);
        if (!io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
            throw new IllegalArgumentException("This type of node [" + connection.getTo().getMetaData().get("UniqueId")
                    + ", " + connection.getTo().getName() + "] only accepts default incoming connection type!");
        }
        if (getFrom() != null && !"true".equals(System.getProperty("jbpm.enable.multi.con"))) {
            throw new IllegalArgumentException("This type of node [" + connection.getTo().getMetaData().get("UniqueId")
                    + ", " + connection.getTo().getName() + "] cannot have more than one incoming connection!");
        }
    }

    public void validateAddOutgoingConnection(final String type, final Connection connection) {
        super.validateAddOutgoingConnection(type, connection);
        if (!io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
            throw new IllegalArgumentException(
                    "This type of node [" + connection.getFrom().getMetaData().get("UniqueId") + ", "
                            + connection.getFrom().getName() + "] only accepts default outgoing connection type!");
        }
        if (getTo() != null && !"true".equals(System.getProperty("jbpm.enable.multi.con"))) {
            throw new IllegalArgumentException(
                    "This type of node [" + connection.getFrom().getMetaData().get("UniqueId") + ", "
                            + connection.getFrom().getName() + "] cannot have more than one outgoing connection!");
        }
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public String getProcessName() {
        return processName;
    }

    public List<Context> getContexts(String contextType) {
        return contextContainer.getContexts(contextType);
    }

    public void addContext(Context context) {
        ((AbstractContext) context).setContextContainer(this);
        contextContainer.addContext(context);
    }

    public Context getContext(String contextType, long id) {
        return contextContainer.getContext(contextType, id);
    }

    public void setDefaultContext(Context context) {
        ((AbstractContext) context).setContextContainer(this);
        contextContainer.setDefaultContext(context);
    }

    public Context getDefaultContext(String contextType) {
        return contextContainer.getDefaultContext(contextType);
    }

    @Override
    public Context getContext(String contextId) {
        Context context = getDefaultContext(contextId);
        if (context != null) {
            return context;
        }
        return super.getContext(contextId);
    }

    public boolean isAbortParent() {

        String abortParent = (String) getMetaData("customAbortParent");
        if (abortParent == null) {
            return true;
        }
        return Boolean.parseBoolean(abortParent);
    }

    public <T> void setSubProcessFactory(SubProcessFactory<T> subProcessFactory) {
        this.subProcessFactory = subProcessFactory;
    }

    public SubProcessFactory getSubProcessFactory() {
        return subProcessFactory;
    }

    public void internalSubProcess(Collection<io.automatik.engine.api.workflow.Process> availableProcesses) {
        io.automatik.engine.api.workflow.Process process = (io.automatik.engine.api.workflow.Process<Map<String, Object>>) availableProcesses
                .stream()
                .filter(p -> p.id().equals(processId)).findFirst().orElse(null);
        if (process == null) {
            return;
        }
        this.subProcessFactory = new SubProcessFactory<Object>() {

            @Override
            public Map<String, Object> bind(ProcessContext ctx) {
                Map<String, Object> parameters = new HashMap<String, Object>();
                for (Iterator<DataAssociation> iterator = getInAssociations().iterator(); iterator.hasNext();) {
                    DataAssociation mapping = iterator.next();
                    Object parameterValue = null;
                    if (mapping.getTransformation() != null) {
                        Transformation transformation = mapping.getTransformation();
                        DataTransformer transformer = DataTransformerRegistry.get().find(transformation.getLanguage());
                        if (transformer != null) {
                            parameterValue = transformer.transform(transformation.getCompiledExpression(),
                                    getSourceParameters(ctx, mapping));

                        }
                    } else {

                        VariableScopeInstance variableScopeInstance = (VariableScopeInstance) ((NodeInstance) ctx
                                .getNodeInstance()).resolveContextInstance(VariableScope.VARIABLE_SCOPE,
                                        mapping.getSources().get(0));
                        if (variableScopeInstance != null) {
                            parameterValue = variableScopeInstance.getVariable(mapping.getSources().get(0));
                        } else {
                            try {
                                parameterValue = MVEL.eval(mapping.getSources().get(0),
                                        new NodeInstanceResolverFactory((NodeInstance) ctx.getNodeInstance()));
                            } catch (Throwable t) {
                                parameterValue = VariableUtil.resolveVariable(mapping.getSources().get(0),
                                        ctx.getNodeInstance());
                                if (parameterValue != null) {
                                    parameters.put(mapping.getTarget(), parameterValue);
                                }
                            }
                        }
                    }
                    if (parameterValue != null) {
                        parameters.put(mapping.getTarget(), parameterValue);
                    }
                }
                return parameters;
            }

            @Override
            public ProcessInstance createInstance(Object model) {
                Model data = (Model) process.createModel();
                data.fromMap((Map<String, Object>) model);

                return process.createInstance(data);
            }

            @Override
            public void unbind(ProcessContext ctx, Object model) {
                Map<String, Object> result = ((Model) model).toMap();
                for (Iterator<DataAssociation> iterator = getOutAssociations().iterator(); iterator
                        .hasNext();) {
                    DataAssociation mapping = iterator.next();
                    if (mapping.getTransformation() != null) {
                        Transformation transformation = mapping.getTransformation();
                        DataTransformer transformer = DataTransformerRegistry.get().find(transformation.getLanguage());
                        if (transformer != null) {
                            Object parameterValue = transformer.transform(transformation.getCompiledExpression(), result);
                            VariableScopeInstance variableScopeInstance = (VariableScopeInstance) ((NodeInstance) ctx
                                    .getNodeInstance()).resolveContextInstance(VariableScope.VARIABLE_SCOPE,
                                            mapping.getTarget());
                            if (variableScopeInstance != null && parameterValue != null) {

                                variableScopeInstance.setVariable(ctx.getNodeInstance(), mapping.getTarget(), parameterValue);
                            }
                        }
                    } else {
                        VariableScopeInstance variableScopeInstance = (VariableScopeInstance) ((NodeInstance) ctx
                                .getNodeInstance()).resolveContextInstance(VariableScope.VARIABLE_SCOPE, mapping.getTarget());
                        if (variableScopeInstance != null) {
                            Object value = result.get(mapping.getSources().get(0));
                            if (value == null) {
                                try {
                                    value = MVEL.eval(mapping.getSources().get(0), result);
                                } catch (Throwable t) {
                                    // do nothing
                                }
                            }
                            variableScopeInstance.setVariable(ctx.getNodeInstance(), mapping.getTarget(), value);
                        } else {
                            String output = mapping.getSources().get(0);
                            String target = mapping.getTarget();

                            Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(target);
                            if (matcher.find()) {
                                String paramName = matcher.group(1);

                                String expression = paramName + " = " + output;
                                Serializable compiled = MVEL.compileExpression(expression);
                                MVEL.executeExpression(compiled, result);
                            }
                        }
                    }
                }
            }

            @SuppressWarnings("unchecked")
            @Override
            public void abortInstance(String instanceId) {
                process.instances().findById(instanceId).ifPresent(pi -> {

                    try {
                        ((ProcessInstance<?>) pi).abort();
                    } catch (IllegalArgumentException e) {
                        // ignore it as this might be thrown in case of canceling already aborted instance
                    }
                });
            }

        };
    }

    protected Map<String, Object> getSourceParameters(ProcessContext ctx, DataAssociation association) {
        Map<String, Object> parameters = new HashMap<String, Object>();
        for (String sourceParam : association.getSources()) {
            Object parameterValue = null;
            VariableScopeInstance variableScopeInstance = (VariableScopeInstance) ((NodeInstance) ctx.getNodeInstance())
                    .resolveContextInstance(VariableScope.VARIABLE_SCOPE, sourceParam);
            if (variableScopeInstance != null) {
                parameterValue = variableScopeInstance.getVariable(sourceParam);
            } else {
                try {
                    parameterValue = MVEL.eval(sourceParam,
                            new NodeInstanceResolverFactory(((NodeInstance) ctx.getNodeInstance())));
                } catch (Throwable t) {

                }
            }
            if (parameterValue != null) {
                parameters.put(association.getTarget(), parameterValue);
            }
        }

        return parameters;
    }

}
