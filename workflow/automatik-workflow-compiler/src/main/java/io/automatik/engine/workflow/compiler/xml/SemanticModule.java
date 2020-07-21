package io.automatik.engine.workflow.compiler.xml;

public interface SemanticModule {
	public String getUri();

	public void addHandler(String name, Handler handler);

	public Handler getHandler(String name);

	public Handler getHandlerByClass(Class<?> clazz);
}
