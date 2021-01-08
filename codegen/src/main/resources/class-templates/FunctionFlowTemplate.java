import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.auth.IdentitySupplier;
import io.automatiko.engine.api.auth.SecurityPolicy;
import io.automatiko.engine.api.runtime.process.WorkflowProcessInstance;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceExecutionException;
import io.automatiko.engine.api.workflow.workitem.Policy;
import io.automatiko.engine.workflow.AbstractProcessInstance;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.funqy.knative.events.CloudEventOutputBuilder;
import io.quarkus.funqy.knative.events.CloudEventOutputBuilder.CloudEventOutput;


public class WorkflowFunction {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("WorkflowFunctionFlow");

    Process<$Type$> process;
    
    Application application;
    
    IdentitySupplier identitySupplier;

    
    public CloudEventOutput<$Type$> startTemplate($Type$Input resource) {
        if (resource == null) {
            resource = new $Type$Input();
        }
        final $Type$Input value = resource;
        identitySupplier.buildIdentityProvider(null, Collections.emptyList());
        return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            String typePrefix = "$TypePrefix$";
            ProcessInstance<$Type$> pi = process.createInstance(null, mapInput(value, new $Type$()));

            pi.start();
            
            String nextNode = (String)((WorkflowProcessInstance)((AbstractProcessInstance<$Type$>) pi).processInstance()).getMetaData("ATK_FUNC_FLOW_NEXT");
            LOGGER.debug("Next function to trigger {}", typePrefix + sanitizeIdentifier(nextNode));
            
            return new CloudEventOutputBuilder().type(typePrefix + "." + sanitizeIdentifier(nextNode)).source(typePrefix)
                    .build(getModel(pi));             
        });
    }
    
    public CloudEventOutput<$Type$> callTemplate($Type$ resource) {        
        AtomicBoolean hasData = new AtomicBoolean(true);
        if (resource == null) {
            resource = new $Type$();
            hasData.set(false);
        }
        final $Type$ value = resource;
        identitySupplier.buildIdentityProvider(null, Collections.emptyList());
        return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            String startFromNode = "$StartFromNode$";
            String typePrefix = "$TypePrefix$";
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
            String nextNode = (String)((WorkflowProcessInstance)((AbstractProcessInstance<$Type$>) pi).processInstance()).getMetaData("ATK_FUNC_FLOW_NEXT");
            LOGGER.debug("Next function to trigger {}", typePrefix + sanitizeIdentifier(nextNode));
            
            return new CloudEventOutputBuilder().type(typePrefix + sanitizeIdentifier(nextNode)).source(typePrefix + sanitizeIdentifier("$ThisNode$"))
            .build(getModel(pi));         
        });
    }
    
    protected $Type$ getModel(ProcessInstance<$Type$> pi) {
        if (pi.status() == ProcessInstance.STATE_ERROR && pi.error().isPresent()) {
            throw new ProcessInstanceExecutionException(pi.id(), pi.error().get().failedNodeId(), pi.error().get().errorMessage());
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

}