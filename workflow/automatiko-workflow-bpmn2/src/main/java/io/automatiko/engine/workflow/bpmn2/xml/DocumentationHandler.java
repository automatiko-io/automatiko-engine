
package io.automatiko.engine.workflow.bpmn2.xml;

import java.util.HashSet;

import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.compiler.xml.BaseAbstractHandler;
import io.automatiko.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatiko.engine.workflow.compiler.xml.Handler;
import io.automatiko.engine.workflow.process.core.impl.NodeImpl;

public class DocumentationHandler extends BaseAbstractHandler implements Handler {

    @SuppressWarnings("unchecked")
    public DocumentationHandler() {
        if ((this.validParents == null) && (this.validPeers == null)) {
            this.validParents = new HashSet();
            this.validParents.add(Object.class);

            this.validPeers = new HashSet();
            this.validPeers.add(null);
            this.validPeers.add(Object.class);

            this.allowNesting = false;
        }
    }

    public Object start(final String uri, final String localName, final Attributes attrs,
            final ExtensibleXmlParser parser) throws SAXException {
        parser.startElementBuilder(localName, attrs);
        return null;
    }

    public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
        Element element = parser.endElementBuilder();
        Object parent = parser.getParent();
        if (parent instanceof NodeImpl) {

            ((NodeImpl) parent).getMetaData().put("Documentation", extractDocumentationText(element));
        } else if (parent instanceof io.automatiko.engine.workflow.base.core.Process) {

            ((io.automatiko.engine.workflow.base.core.Process) parent).getMetaData().put("Documentation",
                    extractDocumentationText(element));
        } else if (parent instanceof Variable) {

            ((Variable) parent).getMetaData().put("Documentation", extractDocumentationText(element));
        }
        return parser.getCurrent();
    }

    public Class<?> generateNodeFor() {
        return null;
    }

    protected String extractDocumentationText(Element element) {
        String text = ((Text) element.getChildNodes().item(0)).getWholeText();
        if (text != null) {
            text = text.trim();
            if ("".equals(text)) {
                text = null;
            }
        }

        return text;
    }
}
