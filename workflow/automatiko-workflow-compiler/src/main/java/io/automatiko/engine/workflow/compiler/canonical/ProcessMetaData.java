
package io.automatiko.engine.workflow.compiler.canonical;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.javaparser.ast.CompilationUnit;

import io.automatiko.engine.api.definition.process.Node;

public class ProcessMetaData {

    private final String processPackageName;
    private final String processBaseClassName;
    private String processClassName;

    private String processId;

    private String extractedProcessId;

    private String processName;

    private String processVersion;

    private String source;

    private CompilationUnit generatedClassModel;

    private Set<String> workItems = new HashSet<>();
    private Map<String, String> subProcesses = new HashMap<>();

    private Map<String, String> signals = new HashMap<>();
    private Map<String, Node> signalNodess = new HashMap<>();

    private List<TriggerMetaData> triggers = new ArrayList<>();

    private List<OpenAPIMetaData> apis = new ArrayList<>();

    private boolean startable;
    private boolean dynamic;

    private Map<String, ServiceTaskDescriptor> generatedHandlers = new HashMap<>();
    private Set<CompilationUnit> generatedListeners = new HashSet<>();

    public ProcessMetaData(String processId, String extractedProcessId, String processName, String processVersion,
            String processPackageName, String processClassName, String source) {
        super();
        this.processId = processId;
        this.extractedProcessId = extractedProcessId;
        this.processName = processName;
        this.processVersion = processVersion;
        this.processPackageName = processPackageName;
        this.processClassName = processPackageName == null ? processClassName
                : processPackageName + "." + processClassName;
        this.processBaseClassName = processClassName;
        this.setSource(source);
    }

    public String getPackageName() {
        return processPackageName;
    }

    public String getProcessBaseClassName() {
        return processBaseClassName;
    }

    public String getProcessClassName() {
        return processClassName;
    }

    public void setProcessClassName(String processClassName) {
        this.processClassName = processClassName;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getExtractedProcessId() {
        return extractedProcessId;
    }

    public void setExtractedProcessId(String extractedProcessId) {
        this.extractedProcessId = extractedProcessId;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public String getProcessVersion() {
        return processVersion;
    }

    public void setProcessVersion(String processVersion) {
        this.processVersion = processVersion;
    }

    public CompilationUnit getGeneratedClassModel() {
        return generatedClassModel;
    }

    public void setGeneratedClassModel(CompilationUnit generatedClassModel) {
        this.generatedClassModel = generatedClassModel;
    }

    public Set<String> getWorkItems() {
        return workItems;
    }

    public Map<String, String> getSubProcesses() {
        return subProcesses;
    }

    public ProcessMetaData addSubProcess(String processId, String subProcessId) {
        subProcesses.put(processId, subProcessId);
        return this;
    }

    public Map<String, ServiceTaskDescriptor> getGeneratedHandlers() {
        return generatedHandlers;
    }

    public ProcessMetaData addGeneratedHandler(String workName, ServiceTaskDescriptor handlerClass) {
        generatedHandlers.put(workName, handlerClass);
        return this;
    }

    public Set<CompilationUnit> getGeneratedListeners() {
        return generatedListeners;
    }

    public void setGeneratedListeners(Set<CompilationUnit> generatedListeners) {
        this.generatedListeners = generatedListeners;
    }

    public Map<String, String> getSignals() {
        return signals;
    }

    public ProcessMetaData addSignal(String name, String value, Node node) {
        signals.put(name, value);
        signalNodess.put(name, node);
        return this;
    }

    public Map<String, Node> getSignalNodes() {
        return signalNodess;
    }

    public List<TriggerMetaData> getTriggers() {
        return triggers;
    }

    public ProcessMetaData addTrigger(TriggerMetaData trigger) {
        triggers.add(trigger);
        return this;
    }

    public List<OpenAPIMetaData> getOpenAPIs() {
        return apis;
    }

    public ProcessMetaData addOpenAPI(OpenAPIMetaData api) {
        apis.add(api);
        return this;
    }

    public boolean isStartable() {
        return startable;
    }

    public void setStartable(boolean startable) {
        this.startable = startable;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    @Override
    public String toString() {
        return "ProcessMetaData [processClassName=" + processClassName + ", processId=" + processId
                + ", extractedProcessId=" + extractedProcessId + ", processName=" + processName + ", processVersion="
                + processVersion + ", workItems=" + workItems + "]";
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

}
