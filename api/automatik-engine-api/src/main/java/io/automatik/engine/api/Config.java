
package io.automatik.engine.api;

import io.automatik.engine.api.decision.DecisionConfig;
import io.automatik.engine.api.workflow.ProcessConfig;

/**
 * Provides general configuration of Kogito application
 */
public interface Config {

	/**
	 * Provides process specific configuration
	 *
	 * @return process specific configuration or null of no process is found in the
	 *         application
	 */
	ProcessConfig process();

	/**
	 * Provides decision specific configuration
	 *
	 * @return decision specific configuration or null of no decision is found in
	 *         the application
	 */
	DecisionConfig decision();

	/**
	 * Provides access to addons in the application.
	 *
	 * @return addons available in the application
	 */
	default Addons addons() {
		return Addons.EMTPY;
	}

}
