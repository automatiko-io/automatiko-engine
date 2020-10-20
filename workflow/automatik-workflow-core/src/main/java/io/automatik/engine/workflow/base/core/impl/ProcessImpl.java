
package io.automatik.engine.workflow.base.core.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.automatik.engine.api.definition.process.WorkflowProcess;
import io.automatik.engine.api.io.Resource;
import io.automatik.engine.workflow.base.core.Context;
import io.automatik.engine.workflow.base.core.ContextContainer;
import io.automatik.engine.workflow.base.core.ContextResolver;
import io.automatik.engine.workflow.base.core.Process;
import io.automatik.engine.workflow.base.core.TagDefinition;
import io.automatik.engine.workflow.base.core.context.AbstractContext;

/**
 * Default implementation of a Process
 * 
 */
public class ProcessImpl implements Process, Serializable, ContextResolver {

    private static final long serialVersionUID = 510l;

    private String id;
    private String name;
    private String version;
    private String type;
    private String visibility;
    private String packageName;
    private Resource resource;
    private ContextContainer contextContainer = new ContextContainerImpl();
    private Map<String, Object> metaData = new HashMap<String, Object>();
    private transient Map<String, Object> runtimeMetaData = new HashMap<String, Object>();
    private Set<String> imports = new HashSet<>();
    private Map<String, String> globals;
    private List<String> functionImports = new ArrayList<>();

    private Collection<TagDefinition> tagDefinitions = new LinkedHashSet<TagDefinition>();

    public void setId(final String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setVersion(final String version) {
        this.version = version;
        if (this.version != null) {
            this.version = version.trim().isEmpty() ? null : version.trim();
        }
    }

    public String getVersion() {
        return this.version;
    }

    public String getType() {
        return this.type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        if (WorkflowProcess.NONE_VISIBILITY.equals(visibility)) {
            // since None is default visibility (process type in bpmn) then treat it
            // as public otherwise nothing will be visible
            visibility = WorkflowProcess.PUBLIC_VISIBILITY;
        }
        this.visibility = visibility;
    }

    public List<Context> getContexts(String contextType) {
        return this.contextContainer.getContexts(contextType);
    }

    public void addContext(Context context) {
        this.contextContainer.addContext(context);
        ((AbstractContext) context).setContextContainer(this);
    }

    public Context getContext(String contextType, long id) {
        return this.contextContainer.getContext(contextType, id);
    }

    public void setDefaultContext(Context context) {
        this.contextContainer.setDefaultContext(context);
        ((AbstractContext) context).setContextContainer(this);
    }

    public Context getDefaultContext(String contextType) {
        return this.contextContainer.getDefaultContext(contextType);
    }

    public boolean equals(final Object o) {
        if (o instanceof ProcessImpl) {
            if (this.id == null) {
                return ((ProcessImpl) o).getId() == null;
            }
            return this.id.equals(((ProcessImpl) o).getId());
        }
        return false;
    }

    public int hashCode() {
        return this.id == null ? 0 : 3 * this.id.hashCode();
    }

    public Context resolveContext(String contextId, Object param) {
        Context context = getDefaultContext(contextId);
        if (context != null) {
            context = context.resolveContext(param);
            if (context != null) {
                return context;
            }
        }
        return null;
    }

    public Map<String, Object> getMetaData() {
        return this.metaData;
    }

    public void setMetaData(String name, Object data) {
        this.metaData.put(name, data);
    }

    public Object getMetaData(String name) {
        return this.metaData.get(name);
    }

    public Resource getResource() {
        return this.resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public Set<String> getImports() {
        return imports;
    }

    public void setImports(Set<String> imports) {
        this.imports = imports;
    }

    public void addImports(Collection<String> imports) {
        this.imports.addAll(imports);
    }

    public List<String> getFunctionImports() {
        return functionImports;
    }

    public void setFunctionImports(List<String> functionImports) {
        this.functionImports = functionImports;
    }

    public void addFunctionImports(Collection<String> functionImports) {
        this.functionImports.addAll(functionImports);
    }

    public Map<String, String> getGlobals() {
        return globals;
    }

    public void setGlobals(Map<String, String> globals) {
        this.globals = globals;
    }

    public String[] getGlobalNames() {
        final List<String> result = new ArrayList<String>();
        if (this.globals != null) {
            for (Iterator<String> iterator = this.globals.keySet().iterator(); iterator.hasNext();) {
                result.add(iterator.next());
            }
        }
        return result.toArray(new String[result.size()]);
    }

    public String getNamespace() {
        return packageName;
    }

    public Map<String, Object> getRuntimeMetaData() {
        return runtimeMetaData;
    }

    public void setRuntimeMetaData(Map<String, Object> runtimeMetaData) {
        this.runtimeMetaData = runtimeMetaData;
    }

    /*
     * Special handling for serialization to initialize transient (runtime related)
     * meta data
     */
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.runtimeMetaData = new HashMap<String, Object>();
    }

    @Override
    public Collection<TagDefinition> getTagDefinitions() {
        return this.tagDefinitions;
    }

    @Override
    public void setTagDefinitions(Collection<TagDefinition> tagDefinitions) {
        this.tagDefinitions = tagDefinitions;
    }
}
