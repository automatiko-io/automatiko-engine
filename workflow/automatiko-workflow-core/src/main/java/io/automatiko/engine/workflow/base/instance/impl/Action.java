
package io.automatiko.engine.workflow.base.instance.impl;

import io.automatiko.engine.api.runtime.process.ProcessContext;

public interface Action {

	void execute(ProcessContext context) throws Exception;

}
