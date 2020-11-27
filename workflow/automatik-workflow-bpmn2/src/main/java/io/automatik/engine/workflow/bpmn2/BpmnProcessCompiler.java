
package io.automatik.engine.workflow.bpmn2;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.automatik.engine.api.definition.process.Node;
import io.automatik.engine.api.definition.process.Process;
import io.automatik.engine.api.io.Resource;
import io.automatik.engine.api.workflow.ProcessConfig;
import io.automatik.engine.workflow.bpmn2.xml.BPMNDISemanticModule;
import io.automatik.engine.workflow.bpmn2.xml.BPMNExtensionsSemanticModule;
import io.automatik.engine.workflow.bpmn2.xml.BPMNSemanticModule;
import io.automatik.engine.workflow.compiler.xml.SemanticModule;
import io.automatik.engine.workflow.compiler.xml.SemanticModules;
import io.automatik.engine.workflow.compiler.xml.XmlProcessReader;
import io.automatik.engine.workflow.process.core.WorkflowProcess;
import io.automatik.engine.workflow.process.core.node.SubProcessNode;

public class BpmnProcessCompiler {

    private final SemanticModules bpmnSemanticModules;

    public BpmnProcessCompiler(SemanticModule... modules) {
        this.bpmnSemanticModules = new SemanticModules();

        if (modules.length == 0) {
            // add default
            this.bpmnSemanticModules.addSemanticModule(new BPMNSemanticModule());
            this.bpmnSemanticModules.addSemanticModule(new BPMNExtensionsSemanticModule());
            this.bpmnSemanticModules.addSemanticModule(new BPMNDISemanticModule());
        } else {
            for (SemanticModule module : modules) {
                this.bpmnSemanticModules.addSemanticModule(module);
            }
        }
    }

    protected SemanticModules getSemanticModules() {
        return bpmnSemanticModules;
    }

    public List<BpmnProcess> from(ProcessConfig config, Resource... resources) {
        try {
            List<Process> processes = parse(config, resources);
            List<BpmnProcess> bpmnProcesses = processes.stream().map(p -> create(p, config))
                    .filter(p -> p != null)
                    .collect(Collectors.toList());

            bpmnProcesses.forEach(p -> {

                for (Node node : ((WorkflowProcess) p.process()).getNodesRecursively()) {

                    processNode(node, bpmnProcesses);
                }
            });

            return (List<BpmnProcess>) bpmnProcesses;
        } catch (Exception e) {
            throw new BpmnProcessReaderException(e);
        }
    }

    public List<Process> parse(Resource... resources) {
        return parse(null, resources);
    }

    public List<Process> parse(ProcessConfig config, Resource... resources) {
        try {
            List<Process> processes = new ArrayList<>();
            XmlProcessReader xmlReader = new XmlProcessReader(getSemanticModules(),
                    Thread.currentThread().getContextClassLoader());
            configureProcessReader(xmlReader, config);

            for (Resource resource : resources) {
                processes.addAll(xmlReader.read(resource.getReader()));
            }

            return processes;
        } catch (Exception e) {
            throw new BpmnProcessReaderException(e);
        }
    }

    protected void configureProcessReader(XmlProcessReader xmlReader, ProcessConfig config) {

    }

    protected BpmnProcess create(Process process, ProcessConfig config) {
        return config == null ? new BpmnProcess(process) : new BpmnProcess(process, config);
    }

    protected void processNode(Node node, List<BpmnProcess> bpmnProcesses) {
        if (node instanceof SubProcessNode) {
            ((SubProcessNode) node).internalSubProcess((List) bpmnProcesses);
        }

    }
}
