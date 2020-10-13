
package io.automatik.engine.quarkus.exception;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import io.automatik.engine.api.workflow.ProcessInstanceDuplicatedException;

@Provider
public class ProcessInstanceDuplicatedExceptionMapper extends BaseExceptionMapper<ProcessInstanceDuplicatedException>
        implements ExceptionMapper<ProcessInstanceDuplicatedException> {

    @Override
    public Response toResponse(ProcessInstanceDuplicatedException ex) {
        ProcessInstanceDuplicatedException exception = (ProcessInstanceDuplicatedException) ex;
        Map<String, String> response = new HashMap<>();
        response.put(MESSAGE, exception.getMessage());
        response.put(PROCESS_INSTANCE_ID, exception.getProcessInstanceId());
        return conflict(response);
    }
}
