package io.automatiko.engine.api.codegen;

import java.util.Map;

public interface AutomatikoConfigProperties {

	Map<String, String> getProperties();

	String getProperty(String name);
}
