
package io.automatiko.engine.workflow.bpmn2.xml;

import java.util.HashSet;

import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import io.automatiko.engine.api.workflow.datatype.DataType;
import io.automatiko.engine.workflow.base.core.ValueObject;
import io.automatiko.engine.workflow.compiler.xml.BaseAbstractHandler;
import io.automatiko.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatiko.engine.workflow.compiler.xml.Handler;

public class MetaValueHandler extends BaseAbstractHandler implements Handler {

	public MetaValueHandler() {
		if ((this.validParents == null) && (this.validPeers == null)) {
			this.validParents = new HashSet<Class<?>>();
			this.validParents.add(ValueObject.class);

			this.validPeers = new HashSet<Class<?>>();
			this.validPeers.add(null);

			this.allowNesting = false;
		}
	}

	public Object start(final String uri, final String localName, final Attributes attrs,
			final ExtensibleXmlParser parser) throws SAXException {
		parser.startElementBuilder(localName, attrs);
		return null;
	}

	public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
		final Element element = parser.endElementBuilder();
		ValueObject valueObject = (ValueObject) parser.getParent();
		String text = ((Text) element.getChildNodes().item(0)).getWholeText();
		if (text != null) {
			text = text.trim();
			if ("".equals(text)) {
				text = null;
			}
		}
		Object value = restoreValue(text, valueObject.getType(), parser);
		valueObject.setValue(value);
		return null;
	}

	private Object restoreValue(String text, DataType dataType, ExtensibleXmlParser parser) throws SAXException {
		if (text == null || "".equals(text)) {
			return null;
		}
		if (dataType == null) {
			throw new SAXParseException("Null datatype", parser.getLocator());
		}
		return dataType.readValue(text);
	}

	public Class<?> generateNodeFor() {
		return null;
	}

}
