
package io.automatiko.engine.addons.predictions.smile;

import java.util.HashMap;
import java.util.Map;

public class RandomForestConfiguration {

	private String outcomeName;
	private AttributeType outcomeType;
	private double confidenceThreshold;
	private int numTrees;
	private Map<String, AttributeType> inputFeatures = new HashMap<>();

	public int getNumTrees() {
		return numTrees;
	}

	public void setNumTrees(int numTrees) {
		this.numTrees = numTrees;
	}

	/**
	 * Returns the name of the output attribute
	 *
	 * @return The name of the output attribute
	 */
	public String getOutcomeName() {
		return outcomeName;
	}

	public void setOutcomeName(String outcomeName) {
		this.outcomeName = outcomeName;
	}

	/**
	 * Returns the type of the output attribute {@link AttributeType}
	 *
	 * @return The type of the output attribute
	 */
	public AttributeType getOutcomeType() {
		return outcomeType;
	}

	public void setOutcomeType(AttributeType outcomeType) {
		this.outcomeType = outcomeType;
	}

	/**
	 * Returns the confidence threshold to use for automatic task completion
	 *
	 * @return The confidence threshold, between 0.0 and 1.0
	 */
	public double getConfidenceThreshold() {
		return confidenceThreshold;
	}

	public void setConfidenceThreshold(double confidenceThreshold) {
		this.confidenceThreshold = confidenceThreshold;
	}

	public Map<String, AttributeType> getInputFeatures() {
		return inputFeatures;
	}

	public void setInputFeatures(Map<String, AttributeType> inputFeatures) {
		this.inputFeatures = inputFeatures;
	}
}
