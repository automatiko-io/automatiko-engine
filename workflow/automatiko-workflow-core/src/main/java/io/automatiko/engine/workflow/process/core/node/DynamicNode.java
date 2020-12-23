
package io.automatiko.engine.workflow.process.core.node;

import static io.automatiko.engine.workflow.process.executable.core.Metadata.CUSTOM_AUTO_START;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.api.runtime.process.ProcessContext;

public class DynamicNode extends CompositeContextNode {

    private static final long serialVersionUID = 510L;

    /**
     * String representation of the activationPredicate. Not used at runtime.
     */
    private String activationCondition;
    /**
     * String representation of the completionPredicate. Not used at runtime.
     */
    private String completionCondition;

    private Predicate<ProcessContext> activationPredicate;
    private Predicate<ProcessContext> completionPredicate;
    private String language;

    public DynamicNode() {
        setAutoComplete(false);
    }

    @Override
    public Node internalGetNode(long id) {
        try {
            return getNode(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<Node> getAutoStartNodes() {
        return Arrays.stream(getNodes())
                .filter(n -> n.getIncomingConnections().isEmpty()
                        && "true".equalsIgnoreCase((String) n.getMetaData().get(CUSTOM_AUTO_START)))
                .collect(Collectors.toList());
    }

    public String getActivationCondition() {
        return activationCondition;
    }

    public void setActivationCondition(String activationCondition) {
        this.activationCondition = activationCondition;
    }

    public String getCompletionCondition() {
        return completionCondition;
    }

    public void setCompletionCondition(String completionCondition) {
        this.completionCondition = completionCondition;
    }

    public DynamicNode setActivationExpression(Predicate<ProcessContext> activationPredicate) {
        this.activationPredicate = activationPredicate;
        return this;
    }

    public DynamicNode setCompletionExpression(Predicate<ProcessContext> copmletionPredicate) {
        this.completionPredicate = copmletionPredicate;
        return this;
    }

    public boolean canActivate(ProcessContext context) {
        return activationPredicate == null || activationPredicate.test(context);
    }

    public boolean canComplete(ProcessContext context) {
        return isAutoComplete() || (completionPredicate != null && completionPredicate.test(context));
    }

    public boolean hasCompletionCondition() {
        return completionPredicate != null;
    }

    @Override
    public boolean acceptsEvent(String type, Object event, Function<String, String> resolver) {
        for (Node node : getNodes()) {
            if ((node.hasMatchingEventListner(type) || resolver.apply(node.getName()).contains(type))
                    && node.getIncomingConnections().isEmpty()) {
                return true;
            }
        }
        return super.acceptsEvent(type, event);
    }

    public boolean acceptsEvent(String type, Object event) {
        for (Node node : getNodes()) {
            if (type.equals(node.getName()) && node.getIncomingConnections().isEmpty()) {
                return true;
            }
        }
        return super.acceptsEvent(type, event);
    }

}
