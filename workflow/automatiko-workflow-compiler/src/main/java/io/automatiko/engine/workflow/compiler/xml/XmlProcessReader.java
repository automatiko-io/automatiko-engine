
package io.automatiko.engine.workflow.compiler.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.workflow.process.executable.core.ExecutableProcess;

//processId, processPkg, processName, processVersion
public class XmlProcessReader {
	private ExtensibleXmlParser parser;
	private final MessageFormat message = new java.text.MessageFormat(
			"Node Info: id:{0} name:{1} \n" + "Parser message: {2}");

	private final MessageFormat messageWithProcessInfo = new java.text.MessageFormat(
			"Process Info: id:{0}, pkg:{1}, name:{2}, version:{3} \n" + "Node Info: id:{4} name:{5} \n"
					+ "Parser message: {6}");

	private List<Process> processes;

	public XmlProcessReader(final SemanticModules modules, ClassLoader classLoader) {
		this(modules, classLoader, null);
	}

	public XmlProcessReader(final SemanticModules modules, ClassLoader classLoader, final SAXParser parser) {
		this.parser = new ExtensibleXmlParser() {
			@Override
			protected String buildPrintMessage(final SAXParseException x) {
				return processParserMessage(super.getParents(), super.getAttrs(), super.buildPrintMessage(x));
			}
		};

		if (parser != null) {
			this.parser.setParser(parser);
		}
		this.parser.setSemanticModules(modules);
		this.parser.setData(new ProcessBuildData());
		this.parser.setClassLoader(classLoader);
	}

	/**
	 * Read a <code>Process</code> from a <code>Reader</code>.
	 *
	 * @param reader The reader containing the rule-set.
	 *
	 * @return The rule-set.
	 */
	public List<Process> read(final Reader reader) throws SAXException, IOException {
		this.processes = ((ProcessBuildData) this.parser.read(reader)).getProcesses();
		return this.processes;
	}

	/**
	 * Read a <code>Process</code> from an <code>InputStream</code>.
	 *
	 * @param inputStream The input-stream containing the rule-set.
	 *
	 * @return The rule-set.
	 */
	public List<Process> read(final InputStream inputStream) throws SAXException, IOException {
		this.processes = ((ProcessBuildData) this.parser.read(inputStream)).getProcesses();
		return this.processes;
	}

	/**
	 * Read a <code>Process</code> from an <code>InputSource</code>.
	 *
	 * @param in The rule-set input-source.
	 *
	 * @return The rule-set.
	 */
	public List<Process> read(final InputSource in) throws SAXException, IOException {
		this.processes = ((ProcessBuildData) this.parser.read(in)).getProcesses();
		return this.processes;
	}

	void setProcesses(final List<Process> processes) {
		this.processes = processes;
	}

	public List<Process> getProcess() {
		return this.processes;
	}

	public ProcessBuildData getProcessBuildData() {
		return (ProcessBuildData) this.parser.getData();
	}

	protected String processParserMessage(LinkedList<Object> parents, Attributes attr, String errorMessage) {
		String nodeId = (attr == null || attr.getValue("id") == null) ? "" : attr.getValue("id");
		String nodeName = (attr == null || attr.getValue("name") == null) ? "" : attr.getValue("name");

		for (Object parent : parents) {
			if (parent != null && parent instanceof ExecutableProcess) {
				ExecutableProcess process = ((ExecutableProcess) parent);
				return messageWithProcessInfo.format(new Object[] { process.getId(), process.getPackageName(),
						process.getName(), process.getVersion(), nodeId, nodeName, errorMessage });
			}
		}

		return message.format(new Object[] { nodeId, nodeName, errorMessage });
	}
}
