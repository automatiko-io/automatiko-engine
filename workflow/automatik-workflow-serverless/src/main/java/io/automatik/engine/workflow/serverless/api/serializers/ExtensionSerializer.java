
package io.automatik.engine.workflow.serverless.api.serializers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.automatik.engine.workflow.serverless.api.interfaces.Extension;

public class ExtensionSerializer extends StdSerializer<Extension> {

	private Map<String, Class<? extends Extension>> extensionsMap = new HashMap<>();

	public ExtensionSerializer() {
		this(Extension.class);
	}

	protected ExtensionSerializer(Class<Extension> t) {
		super(t);
	}

	public void addExtension(String extensionId, Class<? extends Extension> extensionClass) {
		this.extensionsMap.put(extensionId, extensionClass);
	}

	@Override
	public void serialize(Extension extension, JsonGenerator gen, SerializerProvider provider) throws IOException {

		String extensionId = extension.getExtensionId();

		if (extensionsMap.containsKey(extensionId)) {
			// serialize after setting default bean values...
			BeanSerializerFactory.instance
					.createSerializer(provider,
							TypeFactory.defaultInstance().constructType(extensionsMap.get(extensionId)))
					.serialize(extension, gen, provider);
		} else {
			throw new IllegalArgumentException("Extension handler not registered for: " + extensionId);
		}
	}
}