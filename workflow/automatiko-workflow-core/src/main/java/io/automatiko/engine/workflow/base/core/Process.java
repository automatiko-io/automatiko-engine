
package io.automatiko.engine.workflow.base.core;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Process extends io.automatiko.engine.api.definition.process.Process, ContextContainer {

    /**
     * Sets the id of this process. The id should uniquely identify this process.
     * 
     * @param id the id of the process
     */
    void setId(String id);

    /**
     * Sets the name of this process.
     * 
     * @param name the name of this process
     */
    void setName(String name);

    /**
     * Sets the version of this process.
     * 
     * @param version the version of this process
     */
    void setVersion(String version);

    /**
     * Sets the type of this process.
     * 
     * @param type the type of this process
     */
    void setType(String type);

    /**
     * Sets the package name of this RuleFlow process
     * 
     * @param packageName the package name of this RuleFlow process
     */
    void setPackageName(String packageName);

    void setMetaData(String name, Object value);

    /**
     * Returns the imports of this RuleFlow process. They are defined as a List of
     * fully qualified class names.
     * 
     * @return the imports of this RuleFlow process
     */
    Set<String> getImports();

    /**
     * Sets the imports of this RuleFlow process
     * 
     * @param imports the imports as a List of fully qualified class names
     */
    void setImports(Set<String> imports);

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
     * Returns any tags associated with this process definition
     * 
     * @return tag definitions
     */
    Collection<TagDefinition> getTagDefinitions();

    /**
     * Sets tag definition collection
     * 
     * @param tagDefinitions definitions of the tags
     */
    void setTagDefinitions(Collection<TagDefinition> tagDefinitions);

}
