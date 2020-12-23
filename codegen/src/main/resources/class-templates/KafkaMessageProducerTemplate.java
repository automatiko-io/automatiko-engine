package com.myspace.demo;


import java.util.Optional;
import java.util.TimeZone;

import io.automatiko.engine.api.runtime.process.ProcessInstance;
import io.automatiko.engine.workflow.process.instance.WorkflowProcessInstance;
import io.automatiko.engine.api.event.DataEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;

import org.eclipse.microprofile.reactive.messaging.Message;

public class MessageProducer {
    
    @io.smallrye.reactive.messaging.annotations.Broadcast(0)
    org.eclipse.microprofile.reactive.messaging.Emitter emitter;
    
    Optional<Boolean> useCloudEvents = Optional.of(true);
    
    javax.enterprise.inject.Instance<io.automatiko.engine.api.io.OutputConverter<$Type$, Object>> converter;    
    
    @javax.inject.Inject
    ObjectMapper json;

    public void configure() {
		
    }
    
	public void produce(ProcessInstance pi, $Type$ eventData) {
	    emitter.send(io.smallrye.reactive.messaging.kafka.KafkaRecord.of(((WorkflowProcessInstance) pi).getCorrelationKey(), this.marshall(pi, eventData)));
    }
	    
	private Object marshall(ProcessInstance pi, $Type$ eventData) {
	    try {
	        Object payload = eventData;
	        	        
	        if (useCloudEvents.orElse(true)) {

        	    $DataEventType$ event = new $DataEventType$("",
        	                                                    eventData,
        	                                                    pi.getId(),
        	                                                    pi.getParentProcessInstanceId(),
        	                                                    pi.getRootProcessInstanceId(),
        	                                                    pi.getProcessId(),
        	                                                    pi.getRootProcessId(),
        	                                                    String.valueOf(pi.getState()));
        	    if (pi.getReferenceId() != null && !pi.getReferenceId().isEmpty()) {
        	        event.setAutomatikReferenceId(pi.getReferenceId());
        	    }
        	    return json.writeValueAsString(event);
	        } else {
	            
	            if (converter != null && !converter.isUnsatisfied()) {
	                payload = converter.get().convert(eventData);
	            	            
	                return payload;
	            } else {
	                return json.writeValueAsString(eventData);
	            }
	        }
	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }
	}
}