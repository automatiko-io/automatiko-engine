
package io.automatiko.engine.workflow.base.core;

import java.util.List;

/**
 * 
 */
public interface ContextContainer {

	List<Context> getContexts(String contextType);

	void addContext(Context context);

	Context getContext(String contextType, long id);

	void setDefaultContext(Context context);

	Context getDefaultContext(String contextType);
}
