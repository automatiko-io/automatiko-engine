package com.myspace.demo;


import java.nio.charset.StandardCharsets;
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
import io.netty.handler.codec.mqtt.MqttQoS;
import io.smallrye.reactive.messaging.mqtt.MqttMessage;
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
    org.eclipse.microprofile.reactive.messaging.Emitter<byte[]> emitter;
    
    javax.enterprise.inject.Instance<io.automatiko.engine.api.io.OutputConverter<$Type$, byte[]>> converter;    
    
    @javax.inject.Inject
    ObjectMapper json;
    
    @javax.inject.Inject
    io.automatiko.engine.service.metrics.ProcessMessagingMetrics metrics;
    
    @javax.inject.Inject
    io.automatiko.engine.api.audit.Auditor auditor;

    public void configure() {
		
    }
    
	public void produce(ProcessInstance pi, NodeInstance nodeInstance, $Type$ eventData) {
	    
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
	    
	    emitter.send(new MqttOutMessage(io.smallrye.reactive.messaging.mqtt.MqttMessage.of(topic(pi), log(marshall(pi, eventData)), io.netty.handler.codec.mqtt.MqttQoS.AT_LEAST_ONCE, true), ack,
                nack));
	    
	    try {
            boolean success = done.toCompletableFuture().get(30, TimeUnit.SECONDS);
            if (!success) {
                throw new ProcessInstanceInErrorException(pi.getId());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed at sending message (connector=" + CONNECTOR + ", message=" + MESSAGE + ")", e);
        }

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
   
   public class MqttOutMessage implements io.smallrye.reactive.messaging.mqtt.MqttMessage<byte[]> {

       private io.smallrye.reactive.messaging.mqtt.MqttMessage<byte[]> delegate;

       private Supplier<CompletionStage<Void>> ack;

       private Function<Throwable, CompletionStage<Void>> nack;

       public MqttOutMessage(io.smallrye.reactive.messaging.mqtt.MqttMessage<byte[]> delegate,
               Supplier<CompletionStage<Void>> ack, Function<Throwable, CompletionStage<Void>> nack) {
           this.delegate = delegate;
           this.ack = ack;
           this.nack = nack;
       }

       @Override
       public byte[] getPayload() {
           return delegate.getPayload();
       }

       @Override
       public int getMessageId() {
           return delegate.getMessageId();
       }

       @Override
       public MqttQoS getQosLevel() {
           return delegate.getQosLevel();
       }

       @Override
       public boolean isDuplicate() {
           return delegate.isDuplicate();
       }

       @Override
       public boolean isRetain() {
           return delegate.isRetain();
       }

       @Override
       public String getTopic() {
           return delegate.getTopic();
       }

       @Override
       public Supplier getAck() {
           return ack;
       }

       @Override
       public Function getNack() {
           return nack;
       }
   }
}