package com.myspace.demo;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.Response.ResponseBuilder;

import io.automatiko.engine.api.runtime.process.WorkItemNotFoundException;
import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.auth.SecurityPolicy;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessImageNotFoundException;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceExecutionException;
import io.automatiko.engine.api.workflow.WorkItem;
import io.automatiko.engine.api.workflow.workitem.Policy;
import io.automatiko.engine.workflow.Sig;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class $Type$Resource {

    Process<$Type$> subprocess_$name$;

 
 
    @Mutation
    @Description("Deletes $name$ instance with given id")
    public $Type$Output $parentprocessprefix$_delete_$name$(@Name("parentId") String id,
            @Name("id") final String id_$name$,
            @Name("user") final String user, 
            @Name("groups") final List<String> groups) {
        identitySupplier.buildIdentityProvider(user, groups);
        return io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            ProcessInstance<$Type$> pi = subprocess_$name$.instances().findById($parentprocessid$ + ":" + id_$name$, ProcessInstance.STATE_ACTIVE, io.automatiko.engine.api.workflow.ProcessInstanceReadMode.MUTABLE).orElse(null);
            if (pi == null) {
                pi = subprocess_$name$.instances().findById($parentprocessid$ + ":" + id_$name$, ProcessInstance.STATE_ERROR, io.automatiko.engine.api.workflow.ProcessInstanceReadMode.MUTABLE).orElseThrow(() -> new ProcessInstanceNotFoundException(id));
            }
            tracing(pi);
            pi.abort();
            return getSubModel_$name$(pi);
            
        });
    }

    
    protected $Type$Output getSubModel_$name$(ProcessInstance<$Type$> pi) {
        if (pi.status() == ProcessInstance.STATE_ERROR && pi.errors().isPresent()) {
            throw new ProcessInstanceExecutionException(pi.id(), pi.errors().get().failedNodeIds(), pi.errors().get().errorMessages());
        }
        
        return mapOutput(new $Type$Output(), pi.variables(), pi.businessKey(), pi);
    }

    
    protected $Type$ mapInput($Type$Input input, $Type$ resource) {
        resource.fromMap(input.toMap());
        
        return resource;
    }
    
    protected $Type$Output mapOutput($Type$Output output, $Type$ resource, String businessKey, ProcessInstance<$Type$> pi) {
        output.fromMap(businessKey != null ? businessKey: resource.getId(), resource.toMap());
        
        output.setMetadata(pi.metadata());
        
        return output;
    }
}
