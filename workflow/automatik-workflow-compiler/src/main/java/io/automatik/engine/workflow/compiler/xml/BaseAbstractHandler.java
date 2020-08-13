package io.automatik.engine.workflow.compiler.xml;

import java.util.Date;
import java.util.Set;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public abstract class BaseAbstractHandler {
	protected Set<Class<?>> validPeers;
	protected Set<Class<?>> validParents;
	protected boolean allowNesting;

	public Set<Class<?>> getValidParents() {
		return this.validParents;
	}

	public Set<Class<?>> getValidPeers() {
		return this.validPeers;
	}

	public boolean allowNesting() {
		return this.allowNesting;
	}

	public void emptyAttributeCheck(final String element, final String attributeName, final String attribute,
			final ExtensibleXmlParser xmlPackageReader) throws SAXException {
		if (attribute == null || attribute.trim().equals("")) {
			throw new SAXParseException("<" + element + "> requires a '" + attributeName + "' attribute",
					xmlPackageReader.getLocator());
		}
	}

	public void emptyContentCheck(final String element, final String content,
			final ExtensibleXmlParser xmlPackageReader) throws SAXException {
		if (content == null || content.trim().equals("")) {
			throw new SAXParseException("<" + element + "> requires content", xmlPackageReader.getLocator());
		}
	}

	protected Class<?> constructClass(String name) {
		return constructClass(name, Thread.currentThread().getContextClassLoader());
	}

	protected Class<?> constructClass(String name, ClassLoader cl) {
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
			return Class.forName(name, true, cl);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Unable to construct variable from type", e);
		}
	}
}
