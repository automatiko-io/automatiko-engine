
package io.automatik.engine.workflow.marshalling;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.automatik.engine.api.marshalling.ObjectMarshallingStrategy;
import io.automatik.engine.api.runtime.EnvironmentName;
import io.automatik.engine.api.runtime.process.WorkflowProcessInstance;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.workflow.AbstractProcess;
import io.automatik.engine.workflow.AbstractProcessInstance;
import io.automatik.engine.workflow.marshalling.impl.AutomatikMessages;
import io.automatik.engine.workflow.marshalling.impl.ClassObjectMarshallingStrategyAcceptor;
import io.automatik.engine.workflow.marshalling.impl.MarshallerReaderContext;
import io.automatik.engine.workflow.marshalling.impl.PersisterHelper;
import io.automatik.engine.workflow.marshalling.impl.ProcessMarshallerRegistry;
import io.automatik.engine.workflow.marshalling.impl.ProtobufRuleFlowProcessInstanceMarshaller;
import io.automatik.engine.workflow.marshalling.impl.strategies.SerializablePlaceholderResolverStrategy;
import io.automatik.engine.workflow.process.instance.impl.WorkflowProcessInstanceImpl;

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

		io.automatik.engine.api.runtime.process.ProcessInstance pi = ((AbstractProcessInstance<?>) processInstance)
				.internalGetProcessInstance();

		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

			ProcessMarshallerWriteContext context = new ProcessMarshallerWriteContext(baos,
					((io.automatik.engine.workflow.base.instance.ProcessInstance) pi).getProcessRuntime(), null, env);
			context.setProcessInstanceId(pi.getId());
			context.setState(pi.getState());

			String processType = pi.getProcess().getType();
			context.stream.writeUTF(processType);

			io.automatik.engine.workflow.marshalling.impl.ProcessInstanceMarshaller marshaller = ProcessMarshallerRegistry.INSTANCE
					.getMarshaller(processType);

			Object result = marshaller.writeProcessInstance(context, pi);
			if (marshaller instanceof ProtobufRuleFlowProcessInstanceMarshaller && result != null) {
				AutomatikMessages.ProcessInstance _instance = (AutomatikMessages.ProcessInstance) result;
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
		try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
			MarshallerReaderContext context = new MarshallerReaderContext(bais, null,
					Collections.singletonMap(process.id(), ((AbstractProcess<?>) process).process()), this.env);
			ObjectInputStream stream = context.stream;
			String processInstanceType = stream.readUTF();

			io.automatik.engine.workflow.marshalling.impl.ProcessInstanceMarshaller marshaller = ProcessMarshallerRegistry.INSTANCE
					.getMarshaller(processInstanceType);

			WorkflowProcessInstance pi = (WorkflowProcessInstance) marshaller.readProcessInstance(context);

			context.close();

			return pi;
		} catch (Exception e) {
			throw new RuntimeException("Error while unmarshalling process instance", e);
		}
	}

	public ProcessInstance unmarshallProcessInstance(byte[] data, Process process) {
		return ((AbstractProcess) process).createInstance(unmarshallWorkflowProcessInstance(data, process));
	}

	public ProcessInstance unmarshallReadOnlyProcessInstance(byte[] data, Process process) {
		return ((AbstractProcess) process).createReadOnlyInstance(unmarshallWorkflowProcessInstance(data, process));
	}
}
