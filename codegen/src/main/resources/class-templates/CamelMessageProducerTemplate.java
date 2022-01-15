package com.myspace.demo;


import java.util.Optional;
import java.util.TimeZone;

import io.automatiko.engine.api.runtime.process.ProcessInstance;
import io.automatiko.engine.workflow.process.instance.WorkflowProcessInstance;
import io.automatiko.engine.api.event.DataEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;

import org.eclipse.microprofile.reactive.messaging.Message;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class MessageProducer {
    
    org.eclipse.microprofile.reactive.messaging.Emitter<CamelOutMessage> emitter;

    javax.enterprise.inject.Instance<io.automatiko.engine.api.io.OutputConverter<$Type$, Object>> converter;    
    
    @javax.inject.Inject
    ObjectMapper json;
    
    @javax.inject.Inject
    io.automatiko.engine.service.metrics.ProcessMessagingMetrics metrics;

    public void configure() {
		
    }
    
	public void produce(ProcessInstance pi, $Type$ eventData) {
	    metrics.messageProduced(CONNECTOR, MESSAGE, pi.getProcess());
	    
	    io.smallrye.reactive.messaging.camel.OutgoingExchangeMetadata metadata = new io.smallrye.reactive.messaging.camel.OutgoingExchangeMetadata().putProperty("atkInstanceId", 
                ((WorkflowProcessInstance) pi).getCorrelationKey() != null ? ((WorkflowProcessInstance) pi).getCorrelationKey() : ((WorkflowProcessInstance) pi).getId());
	    emitter.send(CamelOutMessage.of(this.marshall(pi, eventData))
	            .addMetadata(headers(pi, metadata)));
    }
	    
	private Object marshall(ProcessInstance pi, $Type$ eventData) {
	    try {
	        Object payload = eventData;
	        
            if (converter != null && !converter.isUnsatisfied()) {
                payload = converter.get().convert(eventData);
            	            
                return payload;
            } else {
                return json.writeValueAsString(eventData);
            }
	        
	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }
	}
	
	public interface CamelOutMessage extends Message {

        static Message of(Object marshall) {

            return new Message() {
                @Override
                public Object getPayload() {
                    return marshall;
                }

            };
        }
    }
	
	protected io.smallrye.reactive.messaging.camel.OutgoingExchangeMetadata headers(ProcessInstance pi, io.smallrye.reactive.messaging.camel.OutgoingExchangeMetadata metadata) {
	    
	    return metadata;
	}
}