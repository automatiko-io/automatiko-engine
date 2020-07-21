package io.automatik.engine.workflow.marshalling.impl;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import io.automatik.engine.api.marshalling.ObjectMarshallingStrategy;
import io.automatik.engine.api.marshalling.ObjectMarshallingStrategyStore;
import io.automatik.engine.api.runtime.EnvironmentName;
import io.automatik.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatik.engine.workflow.marshalling.impl.strategies.ObjectMarshallingStrategyStoreImpl;
import io.automatik.engine.workflow.marshalling.impl.strategies.SerializablePlaceholderResolverStrategy;

public class MarshallerWriteContext extends ObjectOutputStream {
	public final MarshallerWriteContext stream;

	public long clockTime;

	public final PrintStream out = System.out;

	public final ObjectMarshallingStrategyStore objectMarshallingStrategyStore;
	public final Map<ObjectMarshallingStrategy, Integer> usedStrategies;
	public final Map<ObjectMarshallingStrategy, ObjectMarshallingStrategy.Context> strategyContext;

	public final boolean marshalProcessInstances;
	public final boolean marshalWorkItems;
	public final Map<String, Object> env;

	public Object parameterObject;

	public InternalProcessRuntime processRuntime;

	public MarshallerWriteContext(OutputStream stream, InternalProcessRuntime processRuntime,
			ObjectMarshallingStrategyStore resolverStrategyFactory, Map<String, Object> env) throws IOException {
		this(stream, processRuntime, resolverStrategyFactory, true, true, env);
	}

	public MarshallerWriteContext(OutputStream stream, InternalProcessRuntime processRuntime,
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
			this.objectMarshallingStrategyStore = new ObjectMarshallingStrategyStoreImpl(strats);
		} else {
			this.objectMarshallingStrategyStore = resolverStrategyFactory;
		}
		this.usedStrategies = new HashMap<ObjectMarshallingStrategy, Integer>();
		this.strategyContext = new HashMap<ObjectMarshallingStrategy, ObjectMarshallingStrategy.Context>();

		this.marshalProcessInstances = marshalProcessInstances;
		this.marshalWorkItems = marshalWorkItems;
		this.env = env;

	}

	public Integer getStrategyIndex(ObjectMarshallingStrategy strategy) {
		Integer index = usedStrategies.get(strategy);
		if (index == null) {
			index = Integer.valueOf(usedStrategies.size());
			usedStrategies.put(strategy, index);
			strategyContext.put(strategy, strategy.createContext());
		}
		return index;
	}

	public InternalProcessRuntime getProcessRuntime() {
		return processRuntime;
	}

}
