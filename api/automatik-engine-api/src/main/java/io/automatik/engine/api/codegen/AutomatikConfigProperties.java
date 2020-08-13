package io.automatik.engine.api.codegen;

import java.util.Map;

public interface AutomatikConfigProperties {

	Map<String, String> getProperties();

	String getProperty(String name);
}
