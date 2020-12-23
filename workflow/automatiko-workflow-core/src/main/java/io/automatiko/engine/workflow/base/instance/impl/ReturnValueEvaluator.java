
package io.automatiko.engine.workflow.base.instance.impl;

import io.automatiko.engine.api.runtime.process.ProcessContext;

public interface ReturnValueEvaluator {

	public Object evaluate(ProcessContext processContext) throws Exception;
}
