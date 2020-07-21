package io.automatik.engine.workflow.base.core.validation;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import io.automatik.engine.api.definition.process.Process;
import io.automatik.engine.api.io.Resource;
import io.automatik.engine.workflow.process.executable.core.ExecutableProcess;
import io.automatik.engine.workflow.process.executable.core.validation.ExecutableProcessValidator;

public class ProcessValidatorRegistry {

	private static ProcessValidatorRegistry instance;

	private Map<String, ProcessValidator> defaultValidators = new ConcurrentHashMap<String, ProcessValidator>();
	private Set<ProcessValidator> additionalValidators = new CopyOnWriteArraySet<ProcessValidator>();

	private ProcessValidatorRegistry() {
		defaultValidators.put(ExecutableProcess.RULEFLOW_TYPE, ExecutableProcessValidator.getInstance());
	}

	public static ProcessValidatorRegistry getInstance() {
		if (instance == null) {
			instance = new ProcessValidatorRegistry();
		}

		return instance;
	}

	public void registerAdditonalValidator(ProcessValidator validator) {
		this.additionalValidators.add(validator);
	}

	public ProcessValidator getValidator(Process process, Resource resource) {
		if (!additionalValidators.isEmpty()) {
			for (ProcessValidator validator : additionalValidators) {
				boolean accepted = validator.accept(process, resource);
				if (accepted) {
					return validator;
				}
			}
		}

		return defaultValidators.get(process.getType());
	}
}
