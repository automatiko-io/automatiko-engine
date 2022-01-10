package io.automatiko.engine.codegen.process.image;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.api.interfaces.WorkflowDiagram;
import io.serverlessworkflow.diagram.WorkflowDiagramImpl;

public class SvgServerlessProcessImageGenerator implements SvgProcessImageGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SvgServerlessProcessImageGenerator.class);

    private static DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();

    private static TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();

    private Workflow workflow;

    public SvgServerlessProcessImageGenerator(WorkflowProcess workFlowProcess) {
        this.workflow = (Workflow) workFlowProcess.getMetaData().get("SW-Workflow");
    }

    @Override
    public String generate() {
        if (workflow == null) {
            return null;
        }

        WorkflowDiagram workflowDiagram = new WorkflowDiagramImpl().setWorkflow(workflow)
                .setTemplate("automatiko-workflow-template");

        List<String> stateNames = workflow.getStates().stream()
                .map(state -> state.getName())
                .collect(Collectors.toList());

        String diagramSVG = null;
        try {
            diagramSVG = workflowDiagram.getSvgDiagram();

            InputSource is = new InputSource(new StringReader(diagramSVG));

            DocumentBuilder dBuilder = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
            Document doc = dBuilder.parse(is);

            NodeList textNodes = doc.getElementsByTagName("text");
            for (int i = 0; i < textNodes.getLength(); i++) {
                Element text = (Element) textNodes.item(i);
                if (stateNames.contains(text.getTextContent())) {
                    Node sibling = text.getPreviousSibling();
                    while (!sibling.getNodeName().equals("rect")) {
                        sibling = sibling.getPreviousSibling();
                    }

                    ((Element) sibling).setAttribute("id",
                            UUID.nameUUIDFromBytes(text.getTextContent().getBytes(StandardCharsets.UTF_8)).toString());
                }
            }
            ByteArrayOutputStream dmnDocumentStream = new ByteArrayOutputStream();
            Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            transformer.transform(new DOMSource(doc),
                    new StreamResult(new OutputStreamWriter(dmnDocumentStream, StandardCharsets.UTF_8)));

            diagramSVG = new String(dmnDocumentStream.toByteArray());

        } catch (Exception e) {
            LOGGER.warn("Unable to generate workflow image for {} due to {}", workflow.getName(), e.getMessage());
        }

        return diagramSVG;
    }

}
