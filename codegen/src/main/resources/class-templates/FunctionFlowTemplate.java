import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.auth.IdentitySupplier;
import io.automatiko.engine.api.auth.SecurityPolicy;
import io.automatiko.engine.api.runtime.process.WorkflowProcessInstance;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceExecutionException;
import io.automatiko.engine.api.workflow.workitem.Policy;
import io.automatiko.engine.workflow.AbstractProcessInstance;
import io.automatiko.engine.workflow.process.instance.impl.WorkflowProcessInstanceImpl;
import io.automatiko.engine.api.event.EventSource;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
            ((WorkflowProcessInstanceImpl)((AbstractProcessInstance<$Type$>) pi).processInstance()).getMetaData().remove("ATK_FUNC_FLOW_COUNTER");
            return new FunctionContext((List<String>)((WorkflowProcessInstanceImpl)((AbstractProcessInstance<$Type$>) pi).processInstance()).getMetaData().remove("ATK_FUNC_FLOW_NEXT"), getModel(pi));
        });
        if (ctx.nextNodes != null && eventSource != null) {
            
            for (String nextNode : ctx.nextNodes) {
        
                LOGGER.debug("Next function to trigger {}", sanitizeIdentifier(nextNode));
                eventSource.produce(sanitizeIdentifier(nextNode), typePrefix, ctx.model);
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
            ((WorkflowProcessInstanceImpl)((AbstractProcessInstance<$Type$>) pi).processInstance()).getMetaData().remove("ATK_FUNC_FLOW_COUNTER");
            return new FunctionContext((List<String>)((WorkflowProcessInstanceImpl)((AbstractProcessInstance<$Type$>) pi).processInstance()).getMetaData().remove("ATK_FUNC_FLOW_NEXT"), getModel(pi));
        });
        if (ctx.nextNodes != null && eventSource != null) {
            
            for (String nextNode : ctx.nextNodes) {
        
                LOGGER.debug("Next function to trigger {}", sanitizeIdentifier(nextNode));
                eventSource.produce(sanitizeIdentifier(nextNode), typePrefix + sanitizeIdentifier("$ThisNode$"), ctx.model);
            }
        }  
    }
    
    protected $Type$ getModel(ProcessInstance<$Type$> pi) {
        if (pi.status() == ProcessInstance.STATE_ERROR && pi.errors().isPresent()) {
            throw new ProcessInstanceExecutionException(pi.id(), pi.errors().get().failedNodeIds(), pi.errors().get().errorMessages());
        }
        
        return pi.variables();
    }
    
    protected $Type$ mapInput($Type$Input input, $Type$ resource) {
        resource.fromMap(input.toMap());
        
        return resource;
    }
    
    private String sanitizeIdentifier(String name) {
        return name.replaceAll("\\s", "").toLowerCase();
    }
    
    private class FunctionContext {
        
        List<String> nextNodes;
        $Type$ model;
        
        public FunctionContext(List<String> nextNodes, $Type$ model) {
            this.nextNodes = nextNodes;
            this.model = model;
        }
    }

}