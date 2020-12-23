
package io.automatiko.engine.workflow.process.core;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.workflow.base.core.Process;
import io.automatiko.engine.workflow.base.instance.ProcessInstance;

/**
 * Represents a RuleFlow process.
 * 
 */
public interface WorkflowProcess
        extends io.automatiko.engine.api.definition.process.WorkflowProcess, Process, NodeContainer {

    int PROCESS_TYPE = 1;
    int CASE_TYPE = 2;

    /**
     * Returns the imports of this RuleFlow process. They are defined as a List of
     * fully qualified class names.
     * 
     * @return the imports of this RuleFlow process
     */
    Set<String> getImports();

    /**
     * Returns the function imports of this RuleFlow process. They are defined as a
     * List of fully qualified class names.
     * 
     * @return the function imports of this RuleFlow process
     */
    List<String> getFunctionImports();

    /**
     * Sets the imports of this RuleFlow process
     * 
     * @param imports the imports as a List of fully qualified class names
     */
    void setImports(Set<String> imports);

    /**
     * Sets the imports of this RuleFlow process
     * 
     * @param functionImports the imports as a List of fully qualified class names
     */
    void setFunctionImports(List<String> functionImports);

    /**
     * Returns the globals of this RuleFlow process. They are defined as a Map with
     * the name as key and the type as value.
     * 
     * @return the imports of this RuleFlow process
     */
    Map<String, String> getGlobals();

    /**
     * Sets the imports of this RuleFlow process
     * 
     * @param globals the globals as a Map with the name as key and the type as
     *        value
     */
    void setGlobals(Map<String, String> globals);

    /**
     * Returns the names of the globals used in this RuleFlow process
     * 
     * @return the names of the globals of this RuleFlow process
     */
    String[] getGlobalNames();

    /**
     * Returns whether this process will automatically complete if it contains no
     * active node instances anymore
     * 
     * @return the names of the globals of this RuleFlow process
     */
    boolean isAutoComplete();

    boolean isDynamic();

    boolean isExecutable();

    Integer getProcessType();

    List<Node> getNodesRecursively();

    void setExpressionEvaluator(BiFunction<String, ProcessInstance, String> expressionEvaluator);

    String evaluateExpression(String metaData, ProcessInstance processInstance);

}
