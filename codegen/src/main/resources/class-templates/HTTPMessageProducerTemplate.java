package com.myspace.demo;


import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import io.automatiko.engine.api.runtime.process.ProcessInstance;
import io.automatiko.engine.workflow.process.instance.WorkflowProcessInstance;
import io.automatiko.engine.api.event.DataEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class MessageProducer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("MessageProducer");
    
    @io.smallrye.reactive.messaging.annotations.Broadcast(0)
    org.eclipse.microprofile.reactive.messaging.Emitter<String> emitter;
    
    Optional<Boolean> useCloudEvents = Optional.of(false);
        
    Optional<Boolean> useCloudEventsBinary = Optional.of(false);
    
    javax.enterprise.inject.Instance<io.automatiko.engine.api.io.OutputConverter<$Type$, String>> converter;    
    
    @javax.inject.Inject
    ObjectMapper json;
    
    @javax.inject.Inject
    io.automatiko.engine.service.metrics.ProcessMessagingMetrics metrics;

    public void configure() {
		
    }
    
	public void produce(ProcessInstance pi, $Type$ eventData) {
	    metrics.messageProduced(CONNECTOR, MESSAGE, pi.getProcess());
	    
	    io.quarkus.reactivemessaging.http.runtime.OutgoingHttpMetadata metadata = null;
	        
	        if (converter != null && !converter.isUnsatisfied()) {                    
	            
	            metadata = converter.get().metadata(pi, io.quarkus.reactivemessaging.http.runtime.OutgoingHttpMetadata.class);
	        } 
	        if (metadata == null) {
	        
	            io.quarkus.reactivemessaging.http.runtime.OutgoingHttpMetadata.Builder builder = new io.quarkus.reactivemessaging.http.runtime.OutgoingHttpMetadata.Builder();
	                   
	            headers(pi, builder);
	            if (useCloudEvents.orElse(false)) {
	                if (useCloudEventsBinary.orElse(false)) {
	                    builder.addHeader("Content-Type", "application/json; charset=UTF-8");
                        builder.addHeader("ce-specversion", DataEvent.SPEC_VERSION);
                        builder.addHeader("ce-id", UUID.randomUUID().toString());
                        builder.addHeader("ce-source", "/" + pi.getProcessId() + "/" + pi.getId());
                        builder.addHeader("ce-type", "$TriggerType$");
	                    
	                } else {
	                    builder.addHeader("Content-Type", "application/cloudevents+json; charset=UTF-8");
	                }
	            } else {
	                builder.addHeader("Content-Type", "application/json; charset=UTF-8");
	            }
	            metadata = builder.build();
	        }
	        
	        emitter.send(Message.of(log(marshall(pi, eventData))).addMetadata(metadata));
    }
	    
	private String marshall(ProcessInstance pi, $Type$ eventData) {
	    try {
	        Object payload = eventData;
	        	        
	        if (useCloudEvents.orElse(false)) {
        	    String id = UUID.randomUUID().toString();
                String spec = DataEvent.SPEC_VERSION;
                String source = "/" + pi.getProcessId() + "/" + pi.getId();
                String type = "$TriggerType$";
                String subject = null;
                if (useCloudEventsBinary.orElse(false)) {
                    if (converter != null && !converter.isUnsatisfied()) {
                        return converter.get().convert(eventData);
                    } else {
                        return json.writeValueAsString(payload);
                    }                    
                } else {
                    $DataEventType$ event = new $DataEventType$(spec, id, source, type, subject, null, eventData);
                    return json.writeValueAsString(event);
                }
            } else {
                
                if (converter != null && !converter.isUnsatisfied()) {                    
                                
                    return converter.get().convert(eventData);
                } else {
                    return json.writeValueAsString(payload);
                }
            }
	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }
	}
	
	protected void headers(ProcessInstance pi, io.quarkus.reactivemessaging.http.runtime.OutgoingHttpMetadata.Builder builder) {
	    
	}
	
    protected String log(String value) {
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Message to be published with payload '{}'", value);
        }
        return value;
    }	
}