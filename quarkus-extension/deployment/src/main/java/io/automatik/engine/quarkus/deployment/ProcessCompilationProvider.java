package io.automatik.engine.quarkus.deployment;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import io.automatik.engine.codegen.ApplicationGenerator;
import io.automatik.engine.codegen.Generator;
import io.automatik.engine.codegen.process.ProcessCodegen;

public class ProcessCompilationProvider extends AutomatikCompilationProvider {

	@Override
	public Set<String> handledExtensions() {
		return new HashSet<>(asList(".bpmn", ".bpmn2"));
	}

	@Override
	protected Generator addGenerator(ApplicationGenerator appGen, Set<File> filesToCompile, Context context)
			throws IOException {
		return appGen.withGenerator(ProcessCodegen.ofFiles(new ArrayList<>(filesToCompile)))
				.withClassLoader(Thread.currentThread().getContextClassLoader());
	}
}
