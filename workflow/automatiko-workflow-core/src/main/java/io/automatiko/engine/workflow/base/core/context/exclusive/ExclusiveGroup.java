
package io.automatiko.engine.workflow.base.core.context.exclusive;

import io.automatiko.engine.workflow.base.core.Context;
import io.automatiko.engine.workflow.base.core.context.AbstractContext;

public class ExclusiveGroup extends AbstractContext {

	private static final long serialVersionUID = 510l;

	public static final String EXCLUSIVE_GROUP = "ExclusiveGroup";

	public String getType() {
		return EXCLUSIVE_GROUP;
	}

	public Context resolveContext(Object param) {
		return null;
	}

}
