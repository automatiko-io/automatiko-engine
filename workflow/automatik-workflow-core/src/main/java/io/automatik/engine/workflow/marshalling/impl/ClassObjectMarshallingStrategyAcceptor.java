package io.automatik.engine.workflow.marshalling.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.automatik.engine.api.marshalling.ObjectMarshallingStrategyAcceptor;

public class ClassObjectMarshallingStrategyAcceptor implements ObjectMarshallingStrategyAcceptor {

	private static final String STAR = "*";

	public static final ClassObjectMarshallingStrategyAcceptor DEFAULT = new ClassObjectMarshallingStrategyAcceptor(
			new String[] { "*.*" });

	private final Map<String, Object> patterns;

	public ClassObjectMarshallingStrategyAcceptor(String[] patterns) {
		this.patterns = new HashMap<String, Object>();
		for (String pattern : patterns) {
			addPattern(pattern);
		}
	}

	public ClassObjectMarshallingStrategyAcceptor() {
		this.patterns = new HashMap<String, Object>();
	}

	private void addPattern(String pattern) {

		addImportStylePatterns(this.patterns, pattern);
	}

	public boolean accept(Object object) {
		return isMatched(this.patterns, object.getClass().getName());
	}

	@Override
	public String toString() {
		return "ClassObjectMarshallingStrategyAcceptor for " + patterns.keySet();
	}

	public static boolean isMatched(Map<String, Object> patterns, String className) {
		// Array [] object class names are "[x", where x is the first letter of the
		// array type
		// -> NO '.' in class name, thus!
		// see
		// http://download.oracle.com/javase/6/docs/api/java/lang/Class.html#getName%28%29
		String qualifiedNamespace = className;
		String name = className;
		if (className.indexOf('.') > 0) {
			qualifiedNamespace = className.substring(0, className.lastIndexOf('.')).trim();
			name = className.substring(className.lastIndexOf('.') + 1).trim();
		} else if (className.indexOf('[') == 0) {
			qualifiedNamespace = className.substring(0, className.lastIndexOf('['));
		}
		Object object = patterns.get(qualifiedNamespace);
		if (object == null) {
			return true;
		} else if (STAR.equals(object)) {
			return false;
		} else if (patterns.containsKey("*")) {
			// for now we assume if the name space is * then we have a catchall *.* pattern
			return true;
		} else {
			List list = (List) object;
			return !list.contains(name);
		}
	}

	/**
	 * Populates the import style pattern map from give comma delimited string
	 */
	public static void addImportStylePatterns(Map<String, Object> patterns, String str) {
		if (str == null || "".equals(str.trim())) {
			return;
		}

		String[] items = str.split(" ");
		for (String item : items) {
			String qualifiedNamespace = item.substring(0, item.lastIndexOf('.')).trim();
			String name = item.substring(item.lastIndexOf('.') + 1).trim();
			Object object = patterns.get(qualifiedNamespace);
			if (object == null) {
				if (STAR.equals(name)) {
					patterns.put(qualifiedNamespace, STAR);
				} else {
					// create a new list and add it
					List<String> list = new ArrayList<>();
					list.add(name);
					patterns.put(qualifiedNamespace, list);
				}
			} else if (name.equals(STAR)) {
				// if its a STAR now add it anyway, we don't care if it was a STAR or a List
				// before
				patterns.put(qualifiedNamespace, STAR);
			} else {
				// its a list so add it if it doesn't already exist
				List list = (List) object;
				if (!list.contains(name)) {
					list.add(name);
				}
			}
		}
	}
}
