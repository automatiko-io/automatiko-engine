
package io.automatik.engine.workflow.serverless.spi;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatik.engine.workflow.serverless.api.ExpressionEvaluator;

public class ExpressionEvaluatorProvider {

	private Map<String, ExpressionEvaluator> expressionEvaluatorMap = new HashMap<>();
	private static Logger logger = LoggerFactory.getLogger(ExpressionEvaluatorProvider.class);

	public ExpressionEvaluatorProvider() {
		ServiceLoader<ExpressionEvaluator> foundExpressionEvaluators = ServiceLoader.load(ExpressionEvaluator.class);
		foundExpressionEvaluators.forEach(expressionEvaluator -> {
			expressionEvaluatorMap.put(expressionEvaluator.getName(), expressionEvaluator);
			logger.info("Found expression evaluator with name: " + expressionEvaluator.getName());
		});
	}

	private static class LazyHolder {

		static final ExpressionEvaluatorProvider INSTANCE = new ExpressionEvaluatorProvider();
	}

	public static ExpressionEvaluatorProvider getInstance() {
		return LazyHolder.INSTANCE;
	}

	public Map<String, ExpressionEvaluator> get() {
		return expressionEvaluatorMap;
	}
}