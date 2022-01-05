
package io.automatiko.engine.workflow.process.executable.core.factory;

import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.NodeContainer;
import io.automatiko.engine.workflow.process.core.node.Join;
import io.automatiko.engine.workflow.process.executable.core.ExecutableNodeContainerFactory;

public class JoinFactory extends NodeFactory {

    public static final String METHOD_TYPE = "type";
    public static final String METHOD_NUM_COMPLETED = "numberCompleted";

    public JoinFactory(ExecutableNodeContainerFactory nodeContainerFactory, NodeContainer nodeContainer, long id) {
        super(nodeContainerFactory, nodeContainer, id);
    }

    protected Node createNode() {
        return new Join();
    }

    protected Join getJoin() {
        return (Join) getNode();
    }

    @Override
    public JoinFactory name(String name) {
        super.name(name);
        return this;
    }

    public JoinFactory type(int type) {
        getJoin().setType(type);
        return this;
    }

    public JoinFactory numberCompleted(String n) {
        getJoin().setN(n);
        return this;
    }

}
