
package io.automatik.engine.codegen;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.automatik.engine.codegen.metadata.DefaultLabeler;
import io.automatik.engine.codegen.metadata.Labeler;

public abstract class AbstractGenerator implements Generator {

	protected Path projectDirectory;
	protected GeneratorContext context;

	private final List<Labeler> labelers = new ArrayList<>();
	private final DefaultLabeler defaultLabeler = new DefaultLabeler();

	protected AbstractGenerator() {
		this.labelers.add(defaultLabeler);
	}

	@Override
	public void setProjectDirectory(Path projectDirectory) {
		this.projectDirectory = projectDirectory;
	}

	@Override
	public void setContext(GeneratorContext context) {
		this.context = context;
	}

	@Override
	public GeneratorContext context() {
		return this.context;
	}

	public final void addLabeler(Labeler labeler) {
		this.labelers.add(labeler);
	}

	public final void addLabel(final String key, final String value) {
		defaultLabeler.addLabel(key, value);
	}

	@Override
	public final Map<String, String> getLabels() {
		final Map<String, String> labels = new HashMap<>();
		this.labelers.forEach(l -> labels.putAll(l.generateLabels()));
		return labels;
	}

}
