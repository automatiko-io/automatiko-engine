
package io.automatiko.engine.api.definition.process;

import java.util.Map;

import io.automatiko.engine.api.io.Resource;

/**
 * A Process represents one modular piece of business logic that can be executed
 * by a process engine. Different types of processes may exist.
 *
 */
public interface Process {

    public static final String WORKFLOW_TYPE = "Workflow";
    public static final String FUNCTION_TYPE = "Function";
    public static final String FUNCTION_FLOW_TYPE = "FunctionFlow";

    /**
     * The unique id of the Process.
     *
     * @return the id
     */
    String getId();

    /**
     * The name of the Process.
     *
     * @return the name
     */
    String getName();

    /**
     * The version of the Process. You may use your own versioning format (as the
     * version is not interpreted by the engine).
     *
     * @return the version
     */
    String getVersion();

    /**
     * The package name of this process.
     *
     * @return the package name
     */
    String getPackageName();

    /**
     * The type of process. Different types of processes may exist. This defaults to
     * "RuleFlow".
     *
     * @return the type
     */
    String getType();

    /**
     * Meta data associated with this Node.
     */
    Map<String, Object> getMetaData();

    Resource getResource();

    void setResource(Resource res);
}
