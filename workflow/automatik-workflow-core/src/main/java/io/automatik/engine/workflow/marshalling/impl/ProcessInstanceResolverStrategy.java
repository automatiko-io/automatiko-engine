package io.automatik.engine.workflow.marshalling.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import io.automatik.engine.api.definition.process.Process;
import io.automatik.engine.api.marshalling.ObjectMarshallingStrategy;
import io.automatik.engine.api.runtime.process.ProcessInstance;
import io.automatik.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatik.engine.workflow.base.instance.ProcessInstanceManager;
import io.automatik.engine.workflow.base.instance.ProcessRuntimeImpl;
import io.automatik.engine.workflow.base.instance.impl.ProcessInstanceImpl;
import io.automatik.engine.workflow.process.executable.instance.ExecutableProcessInstance;

/**
 * When using this strategy, knowledge session de/marshalling process will make
 * sure that the processInstance is <i>not</i> serialized as a part of the the
 * session/network.
 * </p>
 * Instead, this strategy, which may only be used for {@link ProcessInstance}
 * objects, saves the process instance in the {@link ProcessInstanceManager},
 * and later retrieves it from there.
 * </p>
 * Should a process instance be completed or aborted, it will be restored as an
 * empty RuleFlowProcessInstance with correct id and state completed, yet no
 * internal details.
 * </p>
 * If you're doing tricky things with serialization and persistence, please make
 * sure to remember that the {@link ProcessInstanceManager} cache of process
 * instances is emptied at the end of every transaction (commit).
 */
public class ProcessInstanceResolverStrategy implements ObjectMarshallingStrategy {

	public boolean accept(Object object) {
		if (object instanceof ProcessInstance) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Retrieve the {@link ProcessInstanceManager} object from the ObjectOutput- or
	 * ObjectInputStream. The stream object will secretly also either be a
	 * {@link MarshallerReaderContext} or a {@link MarshallerWriteContext}.
	 * 
	 * @param streamContext The marshaller stream/context.
	 * @return A {@link ProcessInstanceManager} object.
	 */
	public static ProcessInstanceManager retrieveProcessInstanceManager(Object streamContext) {
		ProcessInstanceManager pim = null;
		if (streamContext instanceof MarshallerWriteContext) {
			MarshallerWriteContext context = (MarshallerWriteContext) streamContext;
			pim = ((ProcessRuntimeImpl) context.getProcessRuntime()).getProcessInstanceManager();
		} else if (streamContext instanceof MarshallerReaderContext) {
			MarshallerReaderContext context = (MarshallerReaderContext) streamContext;
			pim = ((ProcessRuntimeImpl) context.getProcessRuntime()).getProcessInstanceManager();
		} else {
			throw new UnsupportedOperationException("Unable to retrieve " + ProcessInstanceManager.class.getSimpleName()
					+ " from " + streamContext.getClass().getName());
		}
		return pim;
	}

	/**
	 * Fill the process instance .kruntime and .process fields with the appropriate
	 * values.
	 * 
	 * @param processInstance
	 * @param streamContext
	 */
	private void connectProcessInstanceToRuntimeAndProcess(ProcessInstance processInstance, Object streamContext) {
		ProcessInstanceImpl processInstanceImpl = (ProcessInstanceImpl) processInstance;
		InternalProcessRuntime runtime = processInstanceImpl.getProcessRuntime();

		// Attach the process if not present
		if (processInstance.getProcess() == null) {
			String processId = processInstance.getProcessId();
			if (processId != null) {
				Process process = runtime.getProcess(processId);
				if (process != null) {
					processInstanceImpl.setProcess(process);
				}
			}
		}
	}

	/**
	 * Retrieve the {@link ProcessInstanceManager} object from the ObjectOutput- or
	 * ObjectInputStream. The stream object will secretly also either be a
	 * {@link MarshallerReaderContext} or a {@link MarshallerWriteContext}.
	 * </p>
	 * The knowledge runtime object is useful in order to reconnect the process
	 * instance to the process and the knowledge runtime object.
	 * 
	 * @param streamContext The marshaller stream/context.
	 * @return A {@link InternalKnowledgeRuntime} object.
	 */
	public static InternalProcessRuntime retrieveKnowledgeRuntime(Object streamContext) {
		InternalProcessRuntime kruntime = null;
		if (streamContext instanceof MarshallerWriteContext) {
			MarshallerWriteContext context = (MarshallerWriteContext) streamContext;
			kruntime = context.getProcessRuntime();
		} else if (streamContext instanceof MarshallerReaderContext) {
			MarshallerReaderContext context = (MarshallerReaderContext) streamContext;
			kruntime = context.getProcessRuntime();
		} else {
			throw new UnsupportedOperationException("Unable to retrieve " + ProcessInstanceManager.class.getSimpleName()
					+ " from " + streamContext.getClass().getName());
		}
		return kruntime;
	}

	public byte[] marshal(Context context, ObjectOutputStream os, Object object) throws IOException {
		ProcessInstance processInstance = (ProcessInstance) object;
		connectProcessInstanceToRuntimeAndProcess(processInstance, os);
		return processInstance.getId().getBytes();
	}

	public Object unmarshal(String dataType, Context context, ObjectInputStream is, byte[] object,
			ClassLoader classloader) throws IOException, ClassNotFoundException {
		String processInstanceId = new String(object);
		ProcessInstanceManager pim = retrieveProcessInstanceManager(is);
		// load it as read only to avoid any updates to the data base
		ProcessInstance processInstance = pim.getProcessInstance(processInstanceId, true);
		if (processInstance == null) {
			ExecutableProcessInstance result = new ExecutableProcessInstance();
			result.setId(processInstanceId);
			result.internalSetState(ProcessInstance.STATE_COMPLETED);
			return result;
		} else {
			connectProcessInstanceToRuntimeAndProcess(processInstance, is);
			return processInstance;
		}
	}

	public Context createContext() {
		// no context needed
		return null;
	}

}
