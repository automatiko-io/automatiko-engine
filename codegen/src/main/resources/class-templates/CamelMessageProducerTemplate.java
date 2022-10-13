package com.myspace.demo;


import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.api.runtime.process.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceInErrorException;
import io.automatiko.engine.workflow.audit.BaseAuditEntry;
import io.automatiko.engine.workflow.process.instance.WorkflowProcessInstance;
import io.automatiko.engine.workflow.process.instance.impl.WorkflowProcessInstanceImpl;
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
    
    org.eclipse.microprofile.reactive.messaging.Emitter<CamelOutMessage> emitter;

    javax.enterprise.inject.Instance<io.automatiko.engine.api.io.OutputConverter<$Type$, Object>> converter;    
    
    @javax.inject.Inject
    ObjectMapper json;
    
    @javax.inject.Inject
    io.automatiko.engine.service.metrics.ProcessMessagingMetrics metrics;
    
    @javax.inject.Inject
    io.automatiko.engine.api.audit.Auditor auditor;

    public void configure() {
		
    }
    
	public void produce(ProcessInstance pi, NodeInstance nodeInstance, $Type$ eventData) {	    
	    
	    io.smallrye.reactive.messaging.camel.OutgoingExchangeMetadata metadata = new io.smallrye.reactive.messaging.camel.OutgoingExchangeMetadata().putProperty("atkInstanceId", 
                ((WorkflowProcessInstance) pi).getCorrelationKey() != null ? ((WorkflowProcessInstance) pi).getCorrelationKey() : ((WorkflowProcessInstance) pi).getId());

        CompletableFuture<Boolean> done = new CompletableFuture<>();
        Supplier<CompletionStage<Void>> ack = () -> {
            LOGGER.debug("Message {} was successfully publishing by connector {}", MESSAGE, CONNECTOR);
            done.complete(true);
            metrics.messageProduced(CONNECTOR, MESSAGE, pi.getProcess());
            Supplier<AuditEntry> entry = () -> BaseAuditEntry.messaging(pi, CONNECTOR, MESSAGE, eventData).add("message",
                    "Workflow instance sent message");
            auditor.publish(entry);
            return CompletableFuture.completedStage(null);
        };
        Function<Throwable, CompletionStage<Void>> nack = (t) -> {
            LOGGER.debug("Message {} publishing by connector {} failed due to {} ", MESSAGE, CONNECTOR, t.getMessage());
            ((WorkflowProcessInstanceImpl) pi)
                    .setErrorState((io.automatiko.engine.workflow.process.instance.NodeInstance) nodeInstance, t);
            done.complete(false);
            metrics.messageProducedFailure(CONNECTOR, MESSAGE, pi.getProcess());
            return CompletableFuture.completedStage(null);
        };
	    
	    emitter.send(CamelOutMessage.of(this.marshall(pi, eventData)).withAck(ack).withNack(nack)
	            .addMetadata(headers(pi, metadata)));

	    try {
            boolean success = done.toCompletableFuture().get(30, TimeUnit.SECONDS);
            if (!success) {
                throw new ProcessInstanceInErrorException(pi.getId());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed at sending message (connector=" + CONNECTOR + ", message=" + MESSAGE + ")", e);
        }
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