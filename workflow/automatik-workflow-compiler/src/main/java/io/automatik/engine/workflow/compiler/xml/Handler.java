package io.automatik.engine.workflow.compiler.xml;

import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public interface Handler {

	Object start(String uri, String localName, Attributes attrs, ExtensibleXmlParser xmlPackageReader)
			throws SAXException;

	Object end(String uri, String localName, ExtensibleXmlParser xmlPackageReader) throws SAXException;

	Set<Class<?>> getValidParents();

	Set<Class<?>> getValidPeers();

	boolean allowNesting();

	Class<?> generateNodeFor();
}
