
package io.automatik.engine.services.correlation;

import java.util.List;

public interface CorrelationKeyFactory {

	CorrelationKey newCorrelationKey(String businessKey);

	CorrelationKey newCorrelationKey(List<String> businessKeys);
}
