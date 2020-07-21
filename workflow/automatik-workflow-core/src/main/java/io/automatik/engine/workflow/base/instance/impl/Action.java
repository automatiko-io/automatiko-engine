
package io.automatik.engine.workflow.base.instance.impl;

import io.automatik.engine.api.runtime.process.ProcessContext;

public interface Action {

	void execute(ProcessContext context) throws Exception;

}
