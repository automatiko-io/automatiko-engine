
package io.automatiko.engine.workflow.process.executable.instance;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import io.automatiko.engine.workflow.base.instance.AbstractProcessInstanceFactory;
import io.automatiko.engine.workflow.base.instance.ProcessInstance;

public class ServerlessExecutableProcessInstanceFactory extends AbstractProcessInstanceFactory implements Externalizable {

    private static final long serialVersionUID = 510l;

    public ProcessInstance createProcessInstance() {
        return new ServerlessExecutableProcessInstance();
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    }

    public void writeExternal(ObjectOutput out) throws IOException {
    }

}
