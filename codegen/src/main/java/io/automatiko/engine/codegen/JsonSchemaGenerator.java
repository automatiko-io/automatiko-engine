
package io.automatiko.engine.codegen;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;

import io.automatiko.engine.api.UserTask;
import io.automatiko.engine.api.UserTaskParam;
import io.automatiko.engine.codegen.GeneratedFile.Type;
import io.automatiko.engine.codegen.json.JsonUtils;

public class JsonSchemaGenerator {

	private Stream<Class<?>> stream;
	private Function<? super Class<?>, String> getSchemaName;
	private Predicate<? super Class<?>> shouldGenSchema;
	private SchemaVersion schemaVersion = SchemaVersion.DRAFT_7;

	public static class Builder {

		private Stream<Class<?>> stream;
		private Function<? super Class<?>, String> getSchemaName;
		private Predicate<? super Class<?>> shouldGenSchema;
		private String schemaVersion;

		public Builder(Stream<Class<?>> stream) {
			this.stream = stream;
		}

		public Builder withSchemaNameFunction(Function<? super Class<?>, String> getSchemaName) {
			this.getSchemaName = getSchemaName;
			return this;
		}

		public Builder withGenSchemaPredicate(Predicate<? super Class<?>> shouldGenSchema) {
			this.shouldGenSchema = shouldGenSchema;
			return this;
		}

		public Builder withSchemaVersion(String schemaVersion) {
			this.schemaVersion = schemaVersion;
			return this;
		}

		public JsonSchemaGenerator build() {
			JsonSchemaGenerator instance = new JsonSchemaGenerator(stream);
			instance.getSchemaName = getSchemaName != null ? getSchemaName : JsonSchemaGenerator::getKey;
			instance.shouldGenSchema = shouldGenSchema != null ? shouldGenSchema : JsonSchemaGenerator::isUserTaskClass;
			if (schemaVersion != null) {
				instance.schemaVersion = SchemaVersion.valueOf(schemaVersion.trim().toUpperCase());
			}
			return instance;
		}
	}

	private JsonSchemaGenerator(Stream<Class<?>> stream) {
		this.stream = stream;
	}

	public Collection<GeneratedFile> generate() throws IOException {
		SchemaGeneratorConfigBuilder builder = new SchemaGeneratorConfigBuilder(schemaVersion, OptionPreset.PLAIN_JSON);
		builder.forTypesInGeneral().withStringFormatResolver(
				target -> target.getSimpleTypeDescription().equals("Date") ? "date-time" : null);
		builder.forFields().withIgnoreCheck(JsonSchemaGenerator::isNotUserTaskParam);
		SchemaGenerator generator = new SchemaGenerator(builder.build());
		ObjectWriter writer = new ObjectMapper().writer();
		Map<String, List<Class<?>>> map = stream.filter(shouldGenSchema).collect(Collectors.groupingBy(getSchemaName));
		Collection<GeneratedFile> files = new ArrayList<>();
		for (Map.Entry<String, List<Class<?>>> entry : map.entrySet()) {
			ObjectNode merged = null;
			for (Class<?> c : entry.getValue()) {
				ObjectNode read = generator.generateSchema(c);
				if (merged == null) {
					merged = read;
				} else {
					JsonUtils.merge(read, merged);
				}
			}
			try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
				writer.writeValue(outputStream, merged);
				files.add(new GeneratedFile(Type.JSON_SCHEMA, entry.getKey() + ".json", outputStream.toByteArray()));
			}
		}
		return files;
	}

	private static String getKey(Class<?> c) {
		UserTask userTask = c.getAnnotation(UserTask.class);

		if (userTask == null) {
			userTask = (UserTask) Stream.of(c.getAnnotations())
					.filter(a -> a.getClass().getCanonicalName().equals(UserTask.class.getCanonicalName())).findFirst()
					.orElse(null);
		}

		return userTask.processName() + "_" + userTask.taskName();
	}

	private static boolean isUserTaskClass(Class<?> c) {

		return c.isAnnotationPresent(UserTask.class);
	}

	private static boolean isNotUserTaskParam(FieldScope fieldScope) {
		return fieldScope.getDeclaringType().getErasedType().isAnnotationPresent(UserTask.class)
				&& fieldScope.getAnnotation(UserTaskParam.class) == null;
	}
}
