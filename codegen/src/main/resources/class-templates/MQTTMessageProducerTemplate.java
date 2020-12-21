package com.myspace.demo;


import java.util.Optional;
import java.util.TimeZone;

import io.automatik.engine.api.runtime.process.ProcessInstance;
import io.automatik.engine.workflow.process.instance.WorkflowProcessInstance;
import io.automatik.engine.api.event.DataEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;

import org.eclipse.microprofile.reactive.messaging.Message;

public class MessageProducer {
    
    org.eclipse.microprofile.reactive.messaging.Emitter emitter;
    
    Optional<Boolean> useCloudEvents = Optional.of(true);
    
    javax.enterprise.inject.Instance<io.automatik.engine.api.io.OutputConverter<$Type$, byte[]>> converter;    
    
    @javax.inject.Inject
    ObjectMapper json;

    public void configure() {
		
    }
    
	public void produce(ProcessInstance pi, $Type$ eventData) {
	    emitter.send(io.smallrye.reactive.messaging.mqtt.MqttMessage.of(topic(pi), this.marshall(pi, eventData), io.netty.handler.codec.mqtt.MqttQoS.AT_LEAST_ONCE, true));
    }
	    
	private byte[] marshall(ProcessInstance pi, $Type$ eventData) {
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
        	    return json.writeValueAsBytes(event);
            } else {
                
                if (converter != null && !converter.isUnsatisfied()) {                    
                                
                    return converter.get().convert(eventData);
                } else {
                    return json.writeValueAsBytes(payload);
                }
            }
	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }
	}
	
	protected String topic(ProcessInstance pi) {
	    return null;
	}
}