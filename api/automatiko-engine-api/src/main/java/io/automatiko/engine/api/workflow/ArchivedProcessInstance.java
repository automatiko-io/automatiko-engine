package io.automatiko.engine.api.workflow;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ArchivedProcessInstance {

    private final String id;
    private final ExportedProcessInstance<?> export;

    private List<ArchivedVariable> variables = new ArrayList<>();

    private List<ArchivedProcessInstance> subInstances = new ArrayList<>();

    public ArchivedProcessInstance(String id, ExportedProcessInstance<?> export) {
        this.id = id;
        this.export = export;
    }

    public void addVariable(ArchivedVariable variable) {
        this.variables.add(variable);
    }

    public List<ArchivedVariable> getVariables() {
        return variables;
    }

    public void setVariables(List<ArchivedVariable> variables) {
        this.variables = variables;
    }

    public void addSubInstance(ArchivedProcessInstance subInstance) {
        this.subInstances.add(subInstance);
    }

    public List<ArchivedProcessInstance> getSubInstances() {
        return subInstances;
    }

    public void setSubInstances(List<ArchivedProcessInstance> subInstances) {
        this.subInstances = subInstances;
    }

    public String getId() {
        return id;
    }

    public ExportedProcessInstance<?> getExport() {
        return export;
    }

    public void writeAsZip(OutputStream output) throws IOException {
        try (ZipOutputStream zipOut = new ZipOutputStream(output)) {
            writeAsZip(output, zipOut, "");
        }
    }

    public void writeAsZip(OutputStream output, ZipOutputStream zipOut, String parent) throws IOException {

        // first put the exported instance
        ZipEntry zipEntry = new ZipEntry(parent + id);
        zipOut.putNextEntry(zipEntry);
        zipOut.write(export.data());
        // next put all variables that are defined
        for (ArchivedVariable variable : variables) {
            ZipEntry varZipEntry = new ZipEntry(parent + variable.getName());
            zipOut.putNextEntry(varZipEntry);
            zipOut.write(variable.data());
        }
        // lastly put sub instances
        for (ArchivedProcessInstance subInstance : subInstances) {
            subInstance.writeAsZip(output, zipOut, parent + subInstance.getId() + File.separator);
        }
    }
}
