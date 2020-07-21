
package io.automatik.engine.addons.predictions.api;

import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatik.engine.api.runtime.process.WorkItem;
import io.automatik.engine.api.runtime.process.WorkItemManager;
import io.automatik.engine.api.workflow.workitem.InvalidLifeCyclePhaseException;
import io.automatik.engine.api.workflow.workitem.LifeCyclePhase;
import io.automatik.engine.api.workflow.workitem.Transition;
import io.automatik.engine.workflow.base.instance.impl.humantask.BaseHumanTaskLifeCycle;
import io.automatik.engine.workflow.base.instance.impl.humantask.HumanTaskWorkItemImpl;
import io.automatik.engine.workflow.base.instance.impl.workitem.Active;
import io.automatik.engine.workflow.base.instance.impl.workitem.Complete;
import io.automatik.engine.workflow.base.instance.impl.workitem.DefaultWorkItemManager;

public class PredictionAwareHumanTaskLifeCycle extends BaseHumanTaskLifeCycle {

	private static final Logger logger = LoggerFactory.getLogger(PredictionAwareHumanTaskLifeCycle.class);

	private PredictionService predictionService;

	public PredictionAwareHumanTaskLifeCycle(PredictionService predictionService) {
		this.predictionService = Objects.requireNonNull(predictionService);
	}

	@Override
	public Map<String, Object> transitionTo(WorkItem workItem, WorkItemManager manager,
			Transition<Map<String, Object>> transition) {
		LifeCyclePhase targetPhase = phaseById(transition.phase());
		if (targetPhase == null) {
			logger.debug("Target life cycle phase '{}' does not exist in {}", transition.phase(),
					this.getClass().getSimpleName());
			throw new InvalidLifeCyclePhaseException(transition.phase());
		}

		HumanTaskWorkItemImpl humanTaskWorkItem = (HumanTaskWorkItemImpl) workItem;
		if (targetPhase.id().equals(Active.ID)) {

			PredictionOutcome outcome = predictionService.predict(workItem, workItem.getParameters());
			logger.debug("Prediction service returned confidence level {} for work item {}",
					outcome.getConfidenceLevel(), humanTaskWorkItem.getId());

			if (outcome.isCertain()) {
				humanTaskWorkItem.getResults().putAll(outcome.getData());
				logger.debug(
						"Prediction service is certain (confidence level {}) on the outputs, completing work item {}",
						outcome.getConfidenceLevel(), humanTaskWorkItem.getId());
				((DefaultWorkItemManager) manager).internalCompleteWorkItem(humanTaskWorkItem);

				return outcome.getData();
			} else if (outcome.isPresent()) {
				logger.debug(
						"Prediction service is NOT certain (confidence level {}) on the outputs, setting recommended outputs on work item {}",
						outcome.getConfidenceLevel(), humanTaskWorkItem.getId());
				humanTaskWorkItem.getResults().putAll(outcome.getData());

			}
		}

		// prediction service does work only on activating tasks
		Map<String, Object> data = super.transitionTo(workItem, manager, transition);
		if (targetPhase.id().equals(Complete.ID)) {
			// upon actual transition train the data if it's completion phase
			predictionService.train(humanTaskWorkItem, workItem.getParameters(), data);
		}
		return data;
	}

}
