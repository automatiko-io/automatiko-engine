package com.myspace.demo;


import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.TimeZone;
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
    org.eclipse.microprofile.reactive.messaging.Emitter<io.smallrye.reactive.messaging.mqtt.MqttMessage> emitter;
    
    javax.enterprise.inject.Instance<io.automatiko.engine.api.io.OutputConverter<$Type$, byte[]>> converter;    
    
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
	    
	    emitter.send(io.smallrye.reactive.messaging.mqtt.MqttMessage.of(topic(pi), log(marshall(pi, eventData)), io.netty.handler.codec.mqtt.MqttQoS.AT_LEAST_ONCE, true));
	    
	    Supplier<AuditEntry> entry = () -> BaseAuditEntry.messaging(pi, CONNECTOR, MESSAGE, eventData)
                .add("message", "Workflow instance sent message");
        auditor.publish(entry);
    }
	    
	private byte[] marshall(ProcessInstance pi, $Type$ eventData) {
	    try {
	        Object payload = eventData;
	        	        
            if (converter != null && !converter.isUnsatisfied()) {                    
                            
                return converter.get().convert(eventData);
            } else {
                return json.writeValueAsBytes(payload);
            }
            
	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }
	}
	
	protected String topic(ProcessInstance pi) {
	    return null;
	}
	
   protected byte[] log(byte[] value) {
       
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Message to be published with payload '{}'", new String(value, StandardCharsets.UTF_8));
        }
        return value;
    }
}