
package io.automatik.engine.workflow.serverless.api.interfaces;

import java.util.List;
import java.util.Map;

import io.automatik.engine.workflow.serverless.api.end.End;
import io.automatik.engine.workflow.serverless.api.error.Error;
import io.automatik.engine.workflow.serverless.api.filters.StateDataFilter;
import io.automatik.engine.workflow.serverless.api.retry.Retry;
import io.automatik.engine.workflow.serverless.api.start.Start;
import io.automatik.engine.workflow.serverless.api.states.DefaultState.Type;
import io.automatik.engine.workflow.serverless.api.transitions.Transition;

public interface State {

	String getId();

	String getName();

	Type getType();

	Start getStart();

	End getEnd();

	StateDataFilter getStateDataFilter();

	String getDataInputSchema();

	String getDataOutputSchema();

	Transition getTransition();

	List<Error> getOnError();

	List<Retry> getRetry();

	Map<String, String> getMetadata();
}