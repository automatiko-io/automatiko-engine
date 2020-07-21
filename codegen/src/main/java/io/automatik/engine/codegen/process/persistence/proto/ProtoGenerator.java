
package io.automatik.engine.codegen.process.persistence.proto;

import java.util.Collection;
import java.util.Date;

public interface ProtoGenerator<T> {

	String INDEX_COMMENT = "@Field(store = Store.YES)";

	Proto generate(String packageName, Collection<T> dataModel, String... headers);

	Proto generate(String messageComment, String fieldComment, String packageName, T dataModel, String... headers);

	Collection<T> extractDataClasses(Collection<T> input, String targetDirectory);

	default String applicabilityByType(String type) {
		if (type.equals("Collection")) {
			return "repeated";
		}

		return "optional";
	}

	default String protoType(String type) {

		if (String.class.getCanonicalName().equals(type) || String.class.getSimpleName().equalsIgnoreCase(type)) {
			return "string";
		} else if (Integer.class.getCanonicalName().equals(type) || "int".equalsIgnoreCase(type)) {
			return "int32";
		} else if (Long.class.getCanonicalName().equals(type) || "long".equalsIgnoreCase(type)) {
			return "int64";
		} else if (Double.class.getCanonicalName().equals(type) || "double".equalsIgnoreCase(type)) {
			return "double";
		} else if (Float.class.getCanonicalName().equals(type) || "float".equalsIgnoreCase(type)) {
			return "float";
		} else if (Boolean.class.getCanonicalName().equals(type) || "boolean".equalsIgnoreCase(type)) {
			return "bool";
		} else if (Date.class.getCanonicalName().equals(type) || "date".equalsIgnoreCase(type)) {
			return "io.automatik.Date";
		}

		return null;
	}

}
