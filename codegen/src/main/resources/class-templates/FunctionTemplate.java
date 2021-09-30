import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.auth.IdentitySupplier;
import io.automatiko.engine.api.auth.SecurityPolicy;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceExecutionException;
import io.automatiko.engine.api.workflow.workitem.Policy;

import java.util.Collections;
import java.util.List;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class WorkflowFunction {

    Process<$Type$> process;
    
    Application application;
    
    IdentitySupplier identitySupplier;

    
    public $Type$Output call($Type$Input resource) {
        if (resource == null) {
            resource = new $Type$Input();
        }
        final $Type$Input value = resource;
        
        identitySupplier.buildIdentityProvider(null, Collections.emptyList());
        return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            ProcessInstance<$Type$> pi = process.createInstance(null, mapInput(value, new $Type$()));

            pi.start();
            return getModel(pi);
        });
    }
    
    protected $Type$Output getModel(ProcessInstance<$Type$> pi) {
        if (pi.status() == ProcessInstance.STATE_ERROR && pi.errors().isPresent()) {
            throw new ProcessInstanceExecutionException(pi.id(), pi.errors().get().failedNodeIds(), pi.errors().get().errorMessages());
        }
        
        return mapOutput(new $Type$Output(), pi.variables(), pi.businessKey());
    }
    
    protected $Type$ mapInput($Type$Input input, $Type$ resource) {
        resource.fromMap(input.toMap());
        
        return resource;
    }
    
    protected $Type$Output mapOutput($Type$Output output, $Type$ resource, String businessKey) {
        output.fromMap(businessKey != null ? businessKey: resource.getId(), resource.toMap());
        
        return output;
    }
}