package com.myspace.demo;


import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
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
    
    @io.smallrye.reactive.messaging.annotations.Broadcast(0)
    org.eclipse.microprofile.reactive.messaging.Emitter<$Type$> emitter;

    @jakarta.inject.Inject
    io.automatiko.engine.service.metrics.ProcessMessagingMetrics metrics;
    
    @jakarta.inject.Inject
    io.automatiko.engine.api.audit.Auditor auditor;

    public void configure() {
		
    }
        
	public void produce(ProcessInstance pi, NodeInstance nodeInstance, $Type$ eventData) {
	    
        CompletableFuture<Boolean> done = new CompletableFuture<>();
        Supplier<CompletionStage<Void>> ack = () -> {
            LOGGER.debug("Message {} was successfully published by connector {}", MESSAGE, CONNECTOR);
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
        
	    Message<$Type$> msg = Message.of(log(marshall(pi, eventData)));
	    
        msg = msg.withAck(ack).withNack(nack);
        
	    emitter.send(msg);
	    
    }
	    
	private $Type$ marshall(ProcessInstance pi, $Type$ eventData) {
	    return eventData;
	}
	
	protected $Type$ log($Type$ value) {
	 
	    if (LOGGER.isDebugEnabled()) {
	        LOGGER.debug("Message to be published with payload '{}'", value);
	    }
	    return value;
	}
	
}