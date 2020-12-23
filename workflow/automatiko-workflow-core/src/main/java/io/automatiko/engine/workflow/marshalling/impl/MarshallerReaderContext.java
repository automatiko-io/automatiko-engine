package io.automatiko.engine.workflow.marshalling.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.HashMap;
import java.util.Map;

import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.api.marshalling.ObjectMarshallingStrategy;
import io.automatiko.engine.api.marshalling.ObjectMarshallingStrategyStore;
import io.automatiko.engine.api.runtime.EnvironmentName;
import io.automatiko.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatiko.engine.workflow.marshalling.impl.strategies.ObjectMarshallingStrategyStoreImpl;
import io.automatiko.engine.workflow.marshalling.impl.strategies.SerializablePlaceholderResolverStrategy;

public class MarshallerReaderContext extends ObjectInputStream {
	public final MarshallerReaderContext stream;

	public final ObjectMarshallingStrategyStore resolverStrategyFactory;
	public final Map<Integer, ObjectMarshallingStrategy> usedStrategies;
	public final Map<ObjectMarshallingStrategy, ObjectMarshallingStrategy.Context> strategyContexts;

	public final boolean marshalProcessInstances;
	public final boolean marshalWorkItems;
	public final Map<String, Object> env;

	public Object parameterObject;
	public ClassLoader classLoader;

	public Map<String, Process> processes = new HashMap<>();

	public InternalProcessRuntime processRuntime;

	public MarshallerReaderContext(InputStream stream, InternalProcessRuntime processRuntime, Map<String, Object> env)
			throws IOException {
		this(stream, processRuntime, null, true, true, env);
	}

	public MarshallerReaderContext(InputStream stream, InternalProcessRuntime processRuntime,
			Map<String, Process> processes, Map<String, Object> env) throws IOException {
		this(stream, processRuntime, null, true, true, env);
		this.processes = processes;
	}

	public MarshallerReaderContext(InputStream stream, InternalProcessRuntime processRuntime,
			ObjectMarshallingStrategyStore resolverStrategyFactory, boolean marshalProcessInstances,
			boolean marshalWorkItems, Map<String, Object> env) throws IOException {
		super(stream);
		this.processRuntime = processRuntime;
		this.stream = this;
		if (resolverStrategyFactory == null) {
			ObjectMarshallingStrategy[] strats = (ObjectMarshallingStrategy[]) env
					.get(EnvironmentName.OBJECT_MARSHALLING_STRATEGIES);
			if (strats == null) {
				strats = new ObjectMarshallingStrategy[] {
						new SerializablePlaceholderResolverStrategy(ClassObjectMarshallingStrategyAcceptor.DEFAULT) };
			}
			this.resolverStrategyFactory = new ObjectMarshallingStrategyStoreImpl(strats);
		} else {
			this.resolverStrategyFactory = resolverStrategyFactory;
		}
		this.usedStrategies = new HashMap<>();
		this.strategyContexts = new HashMap<>();

		this.marshalProcessInstances = marshalProcessInstances;
		this.marshalWorkItems = marshalWorkItems;
		this.env = env;

		this.parameterObject = null;
	}

	@Override
	protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
		String name = desc.getName();
		try {
			if (this.classLoader == null) {

				this.classLoader = getClass().getClassLoader();

			}
			return Class.forName(name, false, this.classLoader);
		} catch (ClassNotFoundException ex) {
			return super.resolveClass(desc);
		}
	}

	public InternalProcessRuntime getProcessRuntime() {
		return processRuntime;
	}
}
