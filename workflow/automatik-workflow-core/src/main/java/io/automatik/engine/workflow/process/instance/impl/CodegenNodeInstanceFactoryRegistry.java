
package io.automatik.engine.workflow.process.instance.impl;

import io.automatik.engine.workflow.process.core.node.SubProcessNode;
import io.automatik.engine.workflow.process.instance.node.LambdaSubProcessNodeInstance;

public class CodegenNodeInstanceFactoryRegistry extends NodeInstanceFactoryRegistry {

	@Override
	protected NodeInstanceFactory get(Class<?> clazz) {
		if (SubProcessNode.class == clazz) {
			return factory(LambdaSubProcessNodeInstance::new);
		}
		return super.get(clazz);
	}
}
