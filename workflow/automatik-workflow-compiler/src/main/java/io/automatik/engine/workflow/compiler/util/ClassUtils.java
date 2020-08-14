package io.automatik.engine.workflow.compiler.util;

import java.util.Date;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

public class ClassUtils {

	public static Class<?> constructClass(String name) {
		return constructClass(name, Thread.currentThread().getContextClassLoader());
	}

	public static Class<?> constructClass(String name, ClassLoader cl) {
		if (name == null) {
			return Object.class;
		}

		switch (name) {
		case "Object":
			return Object.class;
		case "Integer":
			return Integer.class;
		case "Double":
			return Double.class;
		case "Float":
			return Float.class;
		case "Boolean":
			return Boolean.class;
		case "String":
			return String.class;
		case "Date":
			return Date.class;
		default:
			break;
		}

		try {
			return Class.forName(parseClassname(name), true, cl);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Unable to construct variable from type", e);
		}
	}

	public static String parseClassname(String name) {
		ClassOrInterfaceType type = StaticJavaParser.parseClassOrInterfaceType(name);
		if (type.getScope().isPresent()) {
			name = type.getScope().get().toString() + ".";
		} else {
			name = "";
		}
		name += type.getNameAsString();

		return name;
	}
}
