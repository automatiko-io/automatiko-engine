
package io.automatik.engine.workflow.serverless.api;

import java.util.Properties;

public interface WorkflowPropertySource {

	Properties getPropertySource();

	void setPropertySource(Properties source);
}