
package io.automatiko.engine.codegen.decision;

import java.nio.file.Path;

import org.kie.dmn.api.core.DMNModel;

public class DMNResource {
	private final DMNModel dmnModel;
	private final Path path;

	public DMNResource(DMNModel dmnModel, Path path) {
		this.dmnModel = dmnModel;
		this.path = path;
	}

	public DMNModel getDmnModel() {
		return dmnModel;
	}

	public Path getPath() {
		return path;
	}
}
