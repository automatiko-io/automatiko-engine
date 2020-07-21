package io.automatik.engine.workflow.bpmn2.canonical;

import java.io.IOException;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

public class SourceCode extends SimpleJavaFileObject {

	private String contents = null;

	public SourceCode(String className, String contents) throws Exception {
		super(new URI(className), Kind.SOURCE);
		this.contents = contents;
	}

	public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
		return contents;
	}

}
