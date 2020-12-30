
package io.automatiko.engine.workflow.marshalling;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;

import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.marshalling.ObjectMarshallingStrategy;
import io.automatiko.engine.api.runtime.EnvironmentName;
import io.automatiko.engine.api.runtime.process.WorkflowProcessInstance;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.workflow.AbstractProcess;
import io.automatiko.engine.workflow.AbstractProcessInstance;
import io.automatiko.engine.workflow.marshalling.impl.AutomatikoMessages;
import io.automatiko.engine.workflow.marshalling.impl.ClassObjectMarshallingStrategyAcceptor;
import io.automatiko.engine.workflow.marshalling.impl.MarshallerReaderContext;
import io.automatiko.engine.workflow.marshalling.impl.PersisterHelper;
import io.automatiko.engine.workflow.marshalling.impl.ProcessMarshallerRegistry;
import io.automatiko.engine.workflow.marshalling.impl.ProtobufRuleFlowProcessInstanceMarshaller;
import io.automatiko.engine.workflow.marshalling.impl.strategies.SerializablePlaceholderResolverStrategy;
import io.automatiko.engine.workflow.process.instance.impl.WorkflowProcessInstanceImpl;

public class ProcessInstanceMarshaller {

    private Map<String, Object> env = new HashMap<String, Object>();

    public ProcessInstanceMarshaller(ObjectMarshallingStrategy... strategies) {
        ObjectMarshallingStrategy[] strats = null;
        if (strategies == null) {
            strats = new ObjectMarshallingStrategy[] {
                    new SerializablePlaceholderResolverStrategy(ClassObjectMarshallingStrategyAcceptor.DEFAULT) };
        } else {
            strats = new ObjectMarshallingStrategy[strategies.length + 1];
            int i = 0;
            for (ObjectMarshallingStrategy strategy : strategies) {
                strats[i] = strategy;
                i++;
            }
            strats[i] = new SerializablePlaceholderResolverStrategy(ClassObjectMarshallingStrategyAcceptor.DEFAULT);
        }

        env.put(EnvironmentName.OBJECT_MARSHALLING_STRATEGIES, strats);
    }

    public byte[] marhsallProcessInstance(ProcessInstance<?> processInstance) {

        io.automatiko.engine.api.runtime.process.ProcessInstance pi = ((AbstractProcessInstance<?>) processInstance)
                .internalGetProcessInstance();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            ProcessMarshallerWriteContext context = new ProcessMarshallerWriteContext(baos,
                    ((io.automatiko.engine.workflow.base.instance.ProcessInstance) pi).getProcessRuntime(), null, env);
            context.setProcessInstanceId(pi.getId());
            context.setState(pi.getState());

            String processType = pi.getProcess().getType();
            context.stream.writeUTF(processType);

            io.automatiko.engine.workflow.marshalling.impl.ProcessInstanceMarshaller marshaller = ProcessMarshallerRegistry.INSTANCE
                    .getMarshaller(processType);

            Object result = marshaller.writeProcessInstance(context, pi);
            if (marshaller instanceof ProtobufRuleFlowProcessInstanceMarshaller && result != null) {
                AutomatikoMessages.ProcessInstance _instance = (AutomatikoMessages.ProcessInstance) result;
                PersisterHelper.writeToStreamWithHeader(context, _instance);
            }
            context.close();
            ((WorkflowProcessInstanceImpl) pi).disconnect();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error while marshalling process instance", e);
        }
    }

    public WorkflowProcessInstance unmarshallWorkflowProcessInstance(byte[] data, Process<?> process) {
        Map<String, io.automatiko.engine.api.definition.process.Process> processes = new HashMap<String, io.automatiko.engine.api.definition.process.Process>();
        io.automatiko.engine.api.definition.process.Process p = ((AbstractProcess<?>) process).process();
        processes.put(process.id(), p);// this can include version number in the id
        processes.put(p.getId(), p);// this is raw process id as defined in bpmn or so
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
            MarshallerReaderContext context = new MarshallerReaderContext(bais, null, processes, this.env);
            ObjectInputStream stream = context.stream;
            String processInstanceType = stream.readUTF();

            io.automatiko.engine.workflow.marshalling.impl.ProcessInstanceMarshaller marshaller = ProcessMarshallerRegistry.INSTANCE
                    .getMarshaller(processInstanceType);

            WorkflowProcessInstance pi = (WorkflowProcessInstance) marshaller.readProcessInstance(context);

            context.close();

            return pi;
        } catch (Exception e) {
            throw new RuntimeException("Error while unmarshalling process instance", e);
        }
    }

    public ProcessInstance unmarshallProcessInstance(byte[] data, Process process) {
        WorkflowProcessInstance wpi = unmarshallWorkflowProcessInstance(data, process);
        Model model = ((AbstractProcess) process).createModel();

        model.fromMap(wpi.getVariables());
        return ((AbstractProcess) process).createInstance(wpi, model);
    }

    public ProcessInstance unmarshallReadOnlyProcessInstance(byte[] data, Process process) {
        WorkflowProcessInstance wpi = unmarshallWorkflowProcessInstance(data, process);
        Model model = ((AbstractProcess) process).createModel();

        model.fromMap(wpi.getVariables());
        return ((AbstractProcess) process).createReadOnlyInstance(wpi, model);
    }
}
