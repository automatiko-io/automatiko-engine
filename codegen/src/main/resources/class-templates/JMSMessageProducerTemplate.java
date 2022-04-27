package com.myspace.demo;


import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
import java.util.function.Supplier;

import io.automatiko.engine.api.runtime.process.ProcessInstance;
import io.automatiko.engine.workflow.audit.BaseAuditEntry;
import io.automatiko.engine.workflow.process.instance.WorkflowProcessInstance;
import io.automatiko.engine.api.audit.AuditEntry;
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
    
    Optional<Boolean> useCloudEvents = Optional.of(true);
    
    Optional<Boolean> useCloudEventsBinary = Optional.of(false);
    
    javax.enterprise.inject.Instance<io.automatiko.engine.api.io.OutputConverter<$Type$, String>> converter;    
    
    @javax.inject.Inject
    ObjectMapper json;
    
    @javax.inject.Inject
    io.automatiko.engine.service.metrics.ProcessMessagingMetrics metrics;
    
    @javax.inject.Inject
    io.automatiko.engine.api.audit.Auditor auditor;

    public void configure() {
		
    }
    
	public void produce(ProcessInstance pi, $Type$ eventData) {
	    metrics.messageProduced(CONNECTOR, MESSAGE, pi.getProcess());
	    
	    io.smallrye.reactive.messaging.jms.OutgoingJmsMessageMetadata metadata = null;
	    
	    if (converter != null && !converter.isUnsatisfied()) {                    
            
	        metadata = converter.get().metadata(pi, io.smallrye.reactive.messaging.jms.OutgoingJmsMessageMetadata.class);
        } 
	    if (metadata == null) {
	    
    	    io.smallrye.reactive.messaging.jms.OutgoingJmsMessageMetadata.OutgoingJmsMessageMetadataBuilder builder = io.smallrye.reactive.messaging.jms.OutgoingJmsMessageMetadata.builder();
    	    io.smallrye.reactive.messaging.jms.JmsPropertiesBuilder propsBuilder = io.smallrye.reactive.messaging.jms.JmsProperties.builder();
    	    	    
    	    properties(pi, propsBuilder);
    	    if (useCloudEvents.orElse(false)) {
                if (useCloudEventsBinary.orElse(false)) {
                    propsBuilder.with("contentType", "application/json; charset=UTF-8");
                    propsBuilder.with("ce_specversion", DataEvent.SPEC_VERSION);
                    propsBuilder.with("ce_id", UUID.randomUUID().toString());
                    propsBuilder.with("ce_source", "/" + pi.getProcessId() + "/" + pi.getId());
                    propsBuilder.with("ce_type", "$TriggerType$");
                    
                } else {
                    propsBuilder.with("contentType", "application/cloudevents+json; charset=UTF-8");
                }
            }
    	    builder.withProperties(propsBuilder.build());
    	    metadata = builder.build();
        }
	    
	    emitter.send(Message.of(log(marshall(pi, eventData))).addMetadata(metadata));
	    
	    Supplier<AuditEntry> entry = () -> BaseAuditEntry.messaging(pi, CONNECTOR, MESSAGE, eventData)
                .add("message", "Workflow instance sent message");
        auditor.publish(entry);
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
	
   protected io.smallrye.reactive.messaging.jms.JmsPropertiesBuilder properties(ProcessInstance pi, io.smallrye.reactive.messaging.jms.JmsPropertiesBuilder builder) {
        
        return builder;
    }
   
   protected String log(String value) {
       
       if (LOGGER.isDebugEnabled()) {
           LOGGER.debug("Message to be published with payload '{}'", value);
       }
       return value;
   }   
}