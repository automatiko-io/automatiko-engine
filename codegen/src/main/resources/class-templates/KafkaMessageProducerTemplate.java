package com.myspace.demo;


import java.nio.charset.StandardCharsets;
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

    Optional<Boolean> useCloudEvents = Optional.of(true);
    
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
	    
	    io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata metadata = null;
        
        if (converter != null && !converter.isUnsatisfied()) {                    
            
            metadata = converter.get().metadata(pi, io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata.class);
        } 
        if (metadata == null) {
            
            io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata.OutgoingKafkaRecordMetadataBuilder<?> builder = io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata.builder();
            if (useCloudEvents.orElse(false)) {
                org.apache.kafka.common.header.internals.RecordHeaders headers = new org.apache.kafka.common.header.internals.RecordHeaders();
                if (useCloudEventsBinary.orElse(false)) {
                    
                    headers.add("content-type", "application/json; charset=UTF-8".getBytes(StandardCharsets.UTF_8));
                    headers.add("ce_specversion", DataEvent.SPEC_VERSION.getBytes(StandardCharsets.UTF_8));
                    headers.add("ce_id", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
                    headers.add("ce_source", ("/" + pi.getProcessId() + "/" + pi.getId()).getBytes(StandardCharsets.UTF_8));
                    headers.add("ce_type", "$Trigger$".getBytes(StandardCharsets.UTF_8));
                    
                } else {
                    headers.add("content-type", "application/cloudevents+json; charset=UTF-8".getBytes(StandardCharsets.UTF_8));
                }
                builder.withHeaders(headers);
            }
            metadata = builder.build();
        }
        String key = ((WorkflowProcessInstance) pi).getCorrelationKey();
        emitter.send(io.smallrye.reactive.messaging.kafka.KafkaRecord.of(key, log(key, marshall(pi, eventData))).addMetadata(metadata));

    }
	    
	private String marshall(ProcessInstance pi, $Type$ eventData) {
	    try {
	        if (useCloudEvents.orElse(false)) {
                String id = UUID.randomUUID().toString();
                String spec = DataEvent.SPEC_VERSION;
                String source = "/" + pi.getProcessId() + "/" + pi.getId();
                String type = "$Trigger$";
                String subject = null;
                if (useCloudEventsBinary.orElse(false)) {
                    if (converter != null && !converter.isUnsatisfied()) {
                        return converter.get().convert(eventData);
                    } else {
                        return json.writeValueAsString(eventData);
                    }                    
                } else {
                    $DataEventType$ event = new $DataEventType$(spec, id, source, type, subject, null, eventData);
                    return json.writeValueAsString(event);
                }
            } else {
                if (converter != null && !converter.isUnsatisfied()) {
                    return converter.get().convert(eventData);
                	            
                } else {
                    return json.writeValueAsString(eventData);
                }      
            }
	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }
	}
	
    protected String log(String key, String value) {
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Message to be published with key '{}' and payload '{}'", key, value);
        }
        return value;
    }	
	
}