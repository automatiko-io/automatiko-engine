
package io.automatiko.engine.workflow.process.core.node;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Supplier;

import io.automatiko.engine.api.decision.DecisionModel;
import io.automatiko.engine.api.definition.process.Connection;
import io.automatiko.engine.workflow.base.core.Context;
import io.automatiko.engine.workflow.base.core.ContextContainer;
import io.automatiko.engine.workflow.base.core.context.AbstractContext;
import io.automatiko.engine.workflow.base.core.context.variable.Mappable;
import io.automatiko.engine.workflow.base.core.impl.ContextContainerImpl;

/**
 * Default implementation of a RuleSet node.
 */
public class RuleSetNode extends StateBasedNode implements ContextContainer, Mappable {

    public static abstract class RuleType implements Serializable {

        public static Decision decision(String namespace, String model, String decisionServiceName) {
            return decision(namespace, model, decisionServiceName, true);
        }

        public static Decision decision(String namespace, String model, String name, boolean decisionService) {
            return new Decision(namespace, model, name, decisionService);
        }

        protected String name;

        private RuleType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public boolean isRuleFlowGroup() {
            return false;
        }

        public boolean isRuleUnit() {
            return false;
        }

        public boolean isDecision() {
            return false;
        }

        public static class Decision extends RuleType {

            private String namespace;
            private String dName;
            private boolean decisionService;

            private Decision(String namespace, String model, String decisionServiceName) {
                this(namespace, model, decisionServiceName, true);
            }

            private Decision(String namespace, String model, String name, boolean decisionService) {
                super(model);
                this.namespace = namespace;
                this.dName = name;
                this.decisionService = decisionService;
            }

            @Override
            public boolean isDecision() {
                return true;
            }

            public String getNamespace() {
                return namespace;
            }

            public String getModel() {
                return getName();
            }

            public String getDecision() {
                return dName;
            }

            public boolean isDecisionService() {
                return decisionService;

            }

            @Override
            public String toString() {
                return new StringJoiner(", ", Decision.class.getSimpleName() + "[", "]")
                        .add("namespace='" + namespace + "'").add("model='" + name + "'").add("name='" + dName + "'")
                        .add("decisionService='" + decisionService + "'").toString();
            }
        }
    }

    private static final long serialVersionUID = 510l;

    public static final String DMN_LANG = "http://www.jboss.org/drools/dmn";
    public static final String CMMN_DMN_LANG = "http://www.omg.org/spec/CMMN/DecisionType/DMN1";

    private String language = DMN_LANG;

    // NOTE: ContetxInstances are not persisted as current functionality (exception
    // scope) does not require it
    private ContextContainer contextContainer = new ContextContainerImpl();

    private RuleType ruleType;

    private List<DataAssociation> inMapping = new LinkedList<DataAssociation>();
    private List<DataAssociation> outMapping = new LinkedList<DataAssociation>();

    private Map<String, Object> parameters = new HashMap<String, Object>();

    private Supplier<DecisionModel> decisionModel;

    public void setRuleType(RuleType ruleType) {
        this.ruleType = ruleType;
    }

    public RuleType getRuleType() {
        return ruleType;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Supplier<DecisionModel> getDecisionModel() {
        return decisionModel;
    }

    public void setDecisionModel(Supplier<DecisionModel> decisionModel) {
        this.decisionModel = decisionModel;
    }

    public void validateAddIncomingConnection(final String type, final Connection connection) {
        super.validateAddIncomingConnection(type, connection);
        if (!io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
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
        if (!io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
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

    public void addInMapping(String parameterName, String variableName) {
        inMapping.add(new DataAssociation(variableName, parameterName, null, null));
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

    public void addOutAssociation(DataAssociation dataAssociation) {
        outMapping.add(dataAssociation);
    }

    public List<DataAssociation> getOutAssociations() {
        return Collections.unmodifiableList(outMapping);
    }

    public List<DataAssociation> adjustOutMapping(String forEachOutVariable) {
        List<DataAssociation> result = new ArrayList<DataAssociation>();
        if (forEachOutVariable == null) {
            return result;
        }
        Iterator<DataAssociation> it = outMapping.iterator();
        while (it.hasNext()) {
            DataAssociation association = it.next();
            if (forEachOutVariable.equals(association.getTarget())) {
                it.remove();
                result.add(association);
            }
        }

        return result;
    }

    public List<DataAssociation> adjustInMapping(String forEachInVariable) {
        List<DataAssociation> result = new ArrayList<DataAssociation>();
        if (forEachInVariable == null) {
            return result;
        }
        Iterator<DataAssociation> it = inMapping.iterator();
        while (it.hasNext()) {
            DataAssociation association = it.next();
            if (association.getSources().contains(forEachInVariable)) {
                it.remove();
                result.add(association);
            }
        }

        return result;
    }

    public Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public void setParameter(String param, Object value) {
        this.parameters.put(param, value);
    }

    public Object getParameter(String param) {
        return this.parameters.get(param);
    }

    public Object removeParameter(String param) {
        return this.parameters.remove(param);
    }

    public boolean isDMN() {
        return DMN_LANG.equals(language) || CMMN_DMN_LANG.equals(language);
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
}
