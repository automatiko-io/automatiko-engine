
package io.automatiko.engine.workflow.process.core;

import java.io.Serializable;
import java.util.Optional;

import io.automatiko.engine.api.definition.process.Connection;
import io.automatiko.engine.api.definition.process.NodeContainer;
import io.automatiko.engine.workflow.base.core.Contextable;
import io.automatiko.engine.workflow.base.core.VariableExpression;

/**
 * Represents a node in a RuleFlow.
 * 
 */
public interface Node extends io.automatiko.engine.api.definition.process.Node, Contextable, Serializable {

    String CONNECTION_DEFAULT_TYPE = "DEFAULT";

    /**
     * Method for setting the id of the node
     * 
     * @param id the id of the node
     */
    void setId(long id);

    /**
     * Method for setting the name of the node
     * 
     * @param name the name of the node
     */
    void setName(String name);

    String getUniqueId();

    void addIncomingConnection(String type, Connection connection);

    void addOutgoingConnection(String type, Connection connection);

    void removeIncomingConnection(String type, Connection connection);

    void removeOutgoingConnection(String type, Connection connection);

    void setParentContainer(NodeContainer nodeContainer);

    void setMetaData(String name, Object value);

    Optional<ExpressionCondition> getActivationCheck();

    void setActivationCheck(Optional<ExpressionCondition> activationCheck);

    Optional<ExpressionCondition> getCompletionCheck();

    void setCompletionCheck(Optional<ExpressionCondition> completionCheck);

    VariableExpression getVariableExpression();

    void setVariableExpression(VariableExpression variableExpression);

}
