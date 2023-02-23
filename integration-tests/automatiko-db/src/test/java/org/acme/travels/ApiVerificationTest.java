package org.acme.travels;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceReadMode;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.transaction.Transactional;

@QuarkusTest
public class ApiVerificationTest {
 // @formatter:off
    
    @Inject
    @Named("omboarding_1")
    io.automatiko.engine.api.workflow.Process<?> process;
    
    
    @Test
    @Transactional
    public void testProcessExecution() {
       
        Model model = (Model) process.createModel();
        model.fromMap(Collections.singletonMap("name", "joe"));
        ProcessInstance<? extends Model> processinstance = process.createInstance(model);
        
        processinstance.start();
        assertEquals(ProcessInstance.STATE_ACTIVE, processinstance.status());
        
        Collection<?> instances = process.instances().findByIdOrTag(ProcessInstanceReadMode.READ_ONLY, "joe");
        assertEquals(1, instances.size());
        
        processinstance.tags().add("important");
        
        instances = process.instances().findByIdOrTag(ProcessInstanceReadMode.READ_ONLY, "important");
        assertEquals(1, instances.size());
        
        Collection<String> ids = process.instances().locateByIdOrTag("important");
        assertEquals(1, ids.size());
        
        processinstance.tags().remove("important");
        
        instances = process.instances().findByIdOrTag(ProcessInstanceReadMode.READ_ONLY, "important");
        assertEquals(0, instances.size());
        
        processinstance.abort();
        
        assertEquals(ProcessInstance.STATE_ABORTED, processinstance.status());
    }
    
 // @formatter:on
}
