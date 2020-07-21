
package io.automatik.engine.workflow.base.core;

/**
 * 
 */
public interface Contextable {

	void setContext(String contextType, Context context);

	Context getContext(String contextType);

}
