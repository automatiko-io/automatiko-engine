
package io.automatiko.engine.workflow.base.instance.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.definition.process.Connection;
import io.automatiko.engine.workflow.base.core.context.ProcessContext;
import io.automatiko.engine.workflow.base.instance.ProcessInstance;
import io.automatiko.engine.workflow.process.core.Constraint;
import io.automatiko.engine.workflow.process.instance.NodeInstance;

/**
 * Default implementation of a constraint.
 * 
 */
public class ReturnValueConstraintEvaluator implements Constraint, ConstraintEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReturnValueConstraintEvaluator.class);

    private String name;
    private String constraint;
    private int priority;
    private String dialect;
    private String type;
    private boolean isDefault = false;

    public ReturnValueConstraintEvaluator() {
    }

    private ReturnValueEvaluator evaluator;

    public String getConstraint() {
        return this.constraint;
    }

    public void setConstraint(final String constraint) {
        this.constraint = constraint;
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }

    public int getPriority() {
        return this.priority;
    }

    public void setPriority(final int priority) {
        this.priority = priority;
    }

    public String getDialect() {
        return dialect;
    }

    public void setDialect(String dialect) {
        this.dialect = dialect;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public void wire(Object object) {
        setEvaluator((ReturnValueEvaluator) object);
    }

    public void setEvaluator(ReturnValueEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    public ReturnValueEvaluator getReturnValueEvaluator() {
        return this.evaluator;
    }

    public boolean evaluate(NodeInstance instance, Connection connection, Constraint constraint) {
        Object value;
        try {
            ProcessContext context = new ProcessContext(
                    ((ProcessInstance) instance.getProcessInstance()).getProcessRuntime());
            context.setNodeInstance(instance);
            value = this.evaluator.evaluate(context);
        } catch (Exception e) {
            LOGGER.warn("Constraints evaluation for expression {} failed", constraint, e);
            return false;
        }
        if (!(value instanceof Boolean)) {
            LOGGER.warn("Constraints must return boolean values: " + value + " for expression " + constraint);
            return false;
        }
        return ((Boolean) value).booleanValue();
    }

    public void setMetaData(String name, Object value) {
        // Do nothing
    }

    public Object getMetaData(String name) {
        return null;
    }

}
