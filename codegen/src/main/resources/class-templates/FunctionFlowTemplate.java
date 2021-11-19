import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.auth.IdentitySupplier;
import io.automatiko.engine.api.auth.SecurityPolicy;
import io.automatiko.engine.api.runtime.process.WorkflowProcessInstance;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceExecutionException;
import io.automatiko.engine.workflow.Sig;
import io.automatiko.engine.api.workflow.workitem.Policy;
import io.automatiko.engine.workflow.AbstractProcessInstance;
import io.automatiko.engine.workflow.process.instance.impl.WorkflowProcessInstanceImpl;
import io.automatiko.engine.api.event.EventSource;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings({ "rawtypes", "unchecked" })
public class WorkflowFunction {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("WorkflowFunctionFlow");

    Process<$Type$> process;
    
    Application application;
    
    IdentitySupplier identitySupplier;
    
    EventSource eventSource;

    
    public void startTemplate($Type$Input resource) {
        if (resource == null) {
            resource = new $Type$Input();
        }
        String typePrefix = "$TypePrefix$";
        final $Type$Input value = resource;
        identitySupplier.buildIdentityProvider(null, Collections.emptyList());
        FunctionContext ctx = io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            
            ProcessInstance<$Type$> pi = process.createInstance(null, mapInput(value, new $Type$()));

            pi.start();
            String pid = (String)((WorkflowProcessInstanceImpl) ((AbstractProcessInstance<$Type$>) pi).processInstance()).getMetaData().remove("ATK_FUNC_FLOW_ID");
            if (pid == null) {
                pid = id(pi);
            }
            ((WorkflowProcessInstanceImpl)((AbstractProcessInstance<$Type$>) pi).processInstance()).getMetaData().remove("ATK_FUNC_FLOW_COUNTER");
            return new FunctionContext(pid, (List<String>)((WorkflowProcessInstanceImpl)((AbstractProcessInstance<$Type$>) pi).processInstance()).getMetaData().remove("ATK_FUNC_FLOW_NEXT"), getModel(pi));
        });
        if (ctx.nextNodes != null && eventSource != null) {
            
            for (String nextNode : ctx.nextNodes) {
        
                LOGGER.debug("Next function to trigger {}", sanitizeIdentifier(nextNode));
                eventSource.produce(sanitizeIdentifier(nextNode), typePrefix + ctx.id, ctx.model);
            }
        }
    }
    
    public void callTemplate($Type$ resource) {        
        AtomicBoolean hasData = new AtomicBoolean(true);
        if (resource == null) {
            resource = new $Type$();
            hasData.set(false);
        }
        String typePrefix = "$TypePrefix$";
        final $Type$ value = resource;
        identitySupplier.buildIdentityProvider(null, Collections.emptyList());
        FunctionContext ctx = io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            String startFromNode = "$StartFromNode$";
           
            ProcessInstance<$Type$> pi = value.getId() == null ? null : process.instances().findById(value.getId()).orElse(null); 
            if (pi != null) {
                if (hasData.get()) {
                    pi.updateVariables(value);
                }
                pi.triggerNode(startFromNode);
            } else {            
                pi = process.createInstance(value.getId(), value);                
                pi.startFrom(startFromNode);
            }
            String pid = (String)((WorkflowProcessInstanceImpl) ((AbstractProcessInstance<$Type$>) pi).processInstance()).getMetaData().remove("ATK_FUNC_FLOW_ID");
            if (pid == null) {
                pid = id(pi);
            }
            ((WorkflowProcessInstanceImpl)((AbstractProcessInstance<$Type$>) pi).processInstance()).getMetaData().remove("ATK_FUNC_FLOW_COUNTER");
            return new FunctionContext(pid, (List<String>)((WorkflowProcessInstanceImpl)((AbstractProcessInstance<$Type$>) pi).processInstance()).getMetaData().remove("ATK_FUNC_FLOW_NEXT"), getModel(pi));
        });
        if (ctx.nextNodes != null && eventSource != null) {
            
            for (String nextNode : ctx.nextNodes) {
        
                LOGGER.debug("Next function to trigger {}", sanitizeIdentifier(nextNode));
                eventSource.produce(sanitizeIdentifier(nextNode), typePrefix + sanitizeIdentifier("$ThisNode$".toLowerCase()) + ctx.id, ctx.model);
            }
        }  
    }
    
    public void signalTemplate(io.quarkus.funqy.knative.events.CloudEvent<$signalType$> event) {        
        String id = event.extensions().getOrDefault("atkInstanceId", event.subject());
        String correlation = null;
        String typePrefix = "$TypePrefix$";
        final $signalType$ value = event.data();
        identitySupplier.buildIdentityProvider(null, Collections.emptyList());
        Collection<FunctionContext> contexts = io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {            
            List<FunctionContext> executed = new ArrayList<>();
            if (id != null && !id.isEmpty()) {
                ProcessInstance<$Type$> pi = process.instances().findById(id).orElse(null);
                if (pi != null) {
                    pi.send(Sig.of("$signalName$", value, getReferenceId(event)));
                    String pid = (String)((WorkflowProcessInstanceImpl) ((AbstractProcessInstance<$Type$>) pi).processInstance()).getMetaData().remove("ATK_FUNC_FLOW_ID");
                    if (pid == null) {
                        pid = id(pi);
                    }
                    ((WorkflowProcessInstanceImpl)((AbstractProcessInstance<$Type$>) pi).processInstance()).getMetaData().remove("ATK_FUNC_FLOW_COUNTER");
                    executed.add(new FunctionContext(pid, (List<String>)((WorkflowProcessInstanceImpl)((AbstractProcessInstance<$Type$>) pi).processInstance()).getMetaData().remove("ATK_FUNC_FLOW_NEXT"), getModel(pi)));
                }
                return executed;
            } else if (correlation != null && !correlation.isEmpty()) {
                Collection possiblyFound = process.instances().findByIdOrTag(io.automatiko.engine.api.workflow.ProcessInstanceReadMode.MUTABLE, correlation);
                if (!possiblyFound.isEmpty()) {
                    
                    possiblyFound.forEach(pi -> {
                        ProcessInstance pInstance = (ProcessInstance) pi;
                        LOGGER.debug("Found process instance {} matching correlation {}, signaling it", pInstance.id(), correlation);
                        pInstance.send(Sig.of("$signalName$", value, getReferenceId(event)));
                        String pid = (String)((WorkflowProcessInstanceImpl) ((AbstractProcessInstance<$Type$>) pInstance).processInstance()).getMetaData().remove("ATK_FUNC_FLOW_ID");
                        if (pid == null) {
                            pid = id(pInstance);
                        }
                        ((WorkflowProcessInstanceImpl)((AbstractProcessInstance<$Type$>) pi).processInstance()).getMetaData().remove("ATK_FUNC_FLOW_COUNTER");
                        executed.add(new FunctionContext(pid, (List<String>)((WorkflowProcessInstanceImpl)((AbstractProcessInstance<$Type$>) pInstance).processInstance()).getMetaData().remove("ATK_FUNC_FLOW_NEXT"), getModel(pInstance)));
                    });
                }
                return executed;
            }
            
            throw new io.automatiko.engine.api.workflow.ProcessInstanceNotFoundException(id);
        });
        for (FunctionContext ctx : contexts) {
            if (ctx.nextNodes != null && eventSource != null) {
                
                for (String nextNode : ctx.nextNodes) {
            
                    LOGGER.debug("Next function to trigger {}", sanitizeIdentifier(nextNode));
                    eventSource.produce(sanitizeIdentifier(nextNode), typePrefix + sanitizeIdentifier("$ThisNode$".toLowerCase()) + ctx.id, ctx.model);
                }
            }  
        }
    }
    
    protected $Type$ getModel(ProcessInstance<$Type$> pi) {
        if (pi.status() == ProcessInstance.STATE_ERROR && pi.errors().isPresent()) {
            throw new ProcessInstanceExecutionException(pi.id(), pi.errors().get().failedNodeIds(), pi.errors().get().errorMessages());
        }
        
        $Type$ model =  pi.variables();
        model.setId(id(pi));
        
        return model;
    }
    
    protected $Type$ mapInput($Type$Input input, $Type$ resource) {
        resource.fromMap(input.toMap());
        
        return resource;
    }
    
    private String sanitizeIdentifier(String name) {
        return name.replaceAll("\\s", "");
    }
    
    private String getReferenceId(io.quarkus.funqy.knative.events.CloudEvent<?> event) {
        if (event.source() != null && event.source().contains("/")) {
            String referenceId = event.source().split("/")[1];
            return referenceId;
        }
        return null;
    }
    
    private java.lang.String id(ProcessInstance<$Type$> pi) {
        if (pi.parentProcessInstanceId() != null)
            return pi.parentProcessInstanceId() + ":" + pi.id();
        else
            return pi.id();
    }
    
    private class FunctionContext {
        
        List<String> nextNodes;
        $Type$ model;
        String id;
        
        public FunctionContext(String id, List<String> nextNodes, $Type$ model) {
            this.id = "/" + id;
            this.nextNodes = nextNodes;
            this.model = model;
        }
    }

}