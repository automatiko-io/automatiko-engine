package io.automatiko.engine.workflow.builder;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import io.automatiko.engine.workflow.base.core.Work;
import io.automatiko.engine.workflow.base.core.datatype.impl.type.ObjectDataType;
import io.automatiko.engine.workflow.base.core.impl.ParameterDefinitionImpl;
import io.automatiko.engine.workflow.base.core.impl.WorkImpl;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.node.DataAssociation;
import io.automatiko.engine.workflow.process.core.node.WorkItemNode;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;

/**
 * Builder responsible for building an service node
 */
public class ServiceNodeBuilder extends AbstractNodeBuilder {

    private static final String SERVICE_TASK_TYPE = "Service Task";

    private WorkItemNode node;

    private List<String> inputsFromDataObjects = new ArrayList<String>();

    public ServiceNodeBuilder(String name, WorkflowBuilder workflowBuilder) {
        super(workflowBuilder);
        this.node = new WorkItemNode();

        this.node.setId(ids.incrementAndGet());
        this.node.setName(name);
        this.node.setMetaData("UniqueId", generateUiqueId(this.node));
        this.node.setMetaData("Type", SERVICE_TASK_TYPE);
        this.node.setMetaData("Implementation", "##Java");

        Work work = new WorkImpl();
        this.node.setWork(work);
        work.setName(SERVICE_TASK_TYPE);
        work.setParameter("implementation", "java");

        workflowBuilder.get().addNode(node);

        contect();
    }

    /**
     * Configures what class and method should be invoked
     * 
     * @param serviceClass fully qualified class name
     * @param serviceMethod method name
     * @return the builder
     */
    public ServiceNodeBuilder type(String serviceClass, String serviceMethod) {
        this.node.getWork().setParameter("Interface", serviceClass);
        this.node.getWork().setParameter("Operation", serviceMethod);

        this.node.getWork().setParameter("interfaceImplementationRef", serviceClass);

        return this;
    }

    /**
     * Specifies in type safe manner what class is representing the service.
     * 
     * @param <T> Type of the service
     * @param clazz actual class representing the service
     * @return proxied instance of the service to allow to select in type safe way method to be invoked
     */
    @SuppressWarnings("unchecked")
    public <T> T type(Class<T> clazz) {
        this.node.getWork().setParameter("Interface", clazz.getCanonicalName());

        this.node.getWork().setParameter("interfaceImplementationRef", clazz.getCanonicalName());

        ProxyFactory factory = new ProxyFactory();
        if (clazz.isInterface()) {
            factory.setInterfaces(new Class[] { clazz });
        } else {
            factory.setSuperclass(clazz);
        }

        MethodHandler handler = new MethodHandler() {
            @Override
            public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
                if (node.getWork().getParameter("Operation") != null) {
                    return null;
                }
                int index = 0;
                for (Parameter parameter : thisMethod.getParameters()) {

                    String name = parameter.getName();

                    DataAssociation in = new DataAssociation(inputsFromDataObjects.get(index), name, null, null);
                    node.addInAssociation(in);

                    node.getWork()
                            .addParameterDefinition(new ParameterDefinitionImpl(name, new ObjectDataType(parameter.getType())));

                    index++;
                }

                node.getWork().setParameter("Operation", thisMethod.getName());
                return null;

            }
        };
        try {
            return (T) factory.create(new Class<?>[0], new Object[0], handler);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Selects the data object to be used as input of the service method call
     * 
     * @param <T> type of the data object
     * @param name name of the data object
     * @return always returns null as it is a proxy call to collect data not to return real values
     */
    public <T> T fromDataObject(String name) {
        inputsFromDataObjects.add(name);
        return null;
    }

    /**
     * Configures where the value of service method call should be assigned to - what data object should get
     * the value
     * 
     * @param name name of the data object
     * @param value value to be assigned - it is a proxy call to collect it and not to deal with real values
     * @return the builder
     */
    public ServiceNodeBuilder toDataObject(String name, Object value) {

        DataAssociation out = new DataAssociation("Result", name, null, null);
        node.addOutAssociation(out);

        return this;
    }

    /**
     * Adds error handling node that allows to move to different path in case of an error
     * 
     * @param errorCodes list of codes to listen on
     * @return the builder
     */
    public ErrorNodeBuilder onError(String... errorCodes) {
        workflowBuilder.putOnContext(getNode());
        workflowBuilder.putBuilderOnContext(null);
        return new ErrorNodeBuilder("error on " + node.getName(), (String) this.node.getMetaData("UniqueId"), workflowBuilder)
                .errorCodes(errorCodes);
    }

    @Override
    protected Node getNode() {
        return this.node;
    }
}
