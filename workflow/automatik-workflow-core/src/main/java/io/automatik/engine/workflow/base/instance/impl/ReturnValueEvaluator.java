
package io.automatik.engine.workflow.base.instance.impl;

import io.automatik.engine.api.runtime.process.ProcessContext;

public interface ReturnValueEvaluator {

	public Object evaluate(ProcessContext processContext) throws Exception;
}
