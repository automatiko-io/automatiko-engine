
package io.automatik.engine.workflow.compiler.canonical;

import java.text.MessageFormat;
import java.util.ArrayList;

import io.automatik.engine.services.utils.StringUtils;

public class NodeValidator {

	public static NodeValidator of(String nodeType, String nodeId) {
		return new NodeValidator(nodeType, nodeId);
	}

	private final String nodeType;
	private final String nodeId;
	private final ArrayList<String> errors = new ArrayList<>();

	private NodeValidator(String nodeType, String nodeId) {
		this.nodeType = nodeType;
		this.nodeId = nodeId;
	}

	public NodeValidator notEmpty(String name, String value) {
		if (StringUtils.isEmpty(value)) {
			this.errors.add(MessageFormat.format("{0} should not be empty", name));
		}
		return this;
	}

	public void validate() {
		if (!errors.isEmpty()) {
			throw new IllegalArgumentException(MessageFormat.format("Invalid parameters for {0} \"{1}\": {2}", nodeType,
					nodeId, String.join(", ", errors)));
		}
	}
}
