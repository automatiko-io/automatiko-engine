
package io.automatiko.engine.codegen;

import static io.automatiko.engine.codegen.ApplicationGenerator.log;

public interface FileGenerator {
	String generatedFilePath();

	String generate();

	default GeneratedFile generateFile(GeneratedFile.Type fileType) {
		return new GeneratedFile(fileType, generatedFilePath(), log(generate()));
	}

	default boolean validate() {
		return true;
	}

}
