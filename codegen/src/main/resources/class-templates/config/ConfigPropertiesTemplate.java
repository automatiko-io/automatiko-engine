
import java.util.LinkedHashMap;
import java.util.Map;

public class GeneratedAutomatikConfigProperties implements io.automatik.engine.api.codegen.AutomatikConfigProperties {

	private Map<String, String> properties = new LinkedHashMap<String, String>();
	
	public Map<String, String> getProperties() {
		return this.properties;
	}

	public String getProperty(String name) {
		return this.properties.get(name);
	}
}
