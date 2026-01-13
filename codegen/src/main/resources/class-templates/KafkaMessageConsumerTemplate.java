package com.myspace.demo;

import java.util.TimeZone;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.auth.TrustedIdentityProvider;
import io.automatiko.engine.api.event.DataEvent;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceDuplicatedException;
import io.automatiko.engine.workflow.Sig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;

import org.eclipse.microprofile.reactive.messaging.Message;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class $Type$MessageConsumer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("MessageConsumer");

    Process<$Type$> process;

    Application application;
    
    Optional<Boolean> useCloudEvents = Optional.of(false);
    
    jakarta.enterprise.inject.Instance<io.automatiko.engine.api.io.InputConverter<$DataType$>> converter;
    
    @jakarta.inject.Inject
    ObjectMapper json;

    @jakarta.inject.Inject
    io.automatiko.engine.service.metrics.ProcessMessagingMetrics metrics;
    
    public void configure() {

    }
    
    @io.smallrye.reactive.messaging.annotations.Blocking  
	public CompletionStage<Void> consume(Message<?> msg) {
	    metrics.messageReceived(CONNECTOR, MESSAGE, ((io.automatiko.engine.workflow.AbstractProcess<?>)process).process());
	    final String trigger = "$Trigger$";
        try {
            io.smallrye.reactive.messaging.kafka.KafkaRecord<?, ?> record = (io.smallrye.reactive.messaging.kafka.KafkaRecord<?, ?>) msg;
            
            final $DataType$ eventData;
            final $Type$ model;   
            final String correlation;
            LOGGER.debug("Received message with key '{}' and payload '{}'", record.getKey(), msg.getPayload());
            boolean accepted;
            if (useCloudEvents.orElse(false)) {
                $DataEventType$ event;
                String contentType = header(record, "content-type");
                model = new $Type$(); 
                if (contentType != null && contentType.startsWith("application/cloudevents+json")) {
                    // structured
                    event = json.readValue(record.getPayload().toString(), $DataEventType$.class);
                    eventData = event.getData();
                    
                } else {
                    // binary
                    eventData = convert(record, $DataType$.class);
                    event =  new $DataEventType$(header(record, "ce_specversion"), header(record, "ce_id"), header(record, "ce_source"), header(record, "ce_type"), header(record, "ce_subject"), header(record, "ce_time"), eventData);
                    cloudEventsExtensions(msg, event);
                }
                accepted = acceptedEvent(event, msg);
                if (!accepted) {
                    metrics.messageRejected(CONNECTOR, MESSAGE, ((io.automatiko.engine.workflow.AbstractProcess<?>)process).process());
                    LOGGER.debug("Message has been rejected by filter expression");
                    return msg.ack();
                }
                correlation = correlation(event, msg);                  
            } else {
                eventData = convert(msg, $DataType$.class);
                model = new $Type$();  
                
                accepted = acceptedPayload(eventData, msg);
                if (!accepted) {
                    metrics.messageRejected(CONNECTOR, MESSAGE, ((io.automatiko.engine.workflow.AbstractProcess<?>)process).process());
                    LOGGER.debug("Message has been rejected by filter expression");
                    return msg.ack();
                }
                correlation = correlation(eventData, msg);                 
            }   
            
            IdentityProvider.set(new TrustedIdentityProvider("System<messaging>"));
            io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {                
                
            	if (correlation != null) {
            		LOGGER.debug("Correlation ({}) is set, attempting to find if there is matching instance already active", correlation);
            		Collection possiblyFound = process.instances().findByIdOrTag(io.automatiko.engine.api.workflow.ProcessInstanceReadMode.MUTABLE_WITH_LOCK, correlation);
                    if (!possiblyFound.isEmpty()) {
                        metrics.messageConsumed(CONNECTOR, MESSAGE, ((io.automatiko.engine.workflow.AbstractProcess<?>)process).process());
                        possiblyFound.forEach(pi -> {
                            ProcessInstance pInstance = (ProcessInstance) pi;
                            LOGGER.debug("Found process instance {} matching correlation {}, signaling instead of starting new instance", pInstance.id(), correlation);
                            pInstance.send(Sig.of(canStartInstance() ? trigger : "Message-" + trigger, eventData));
                        });
                        return null;
                    }
            		
                }
            	if (canStartInstance()) {
                    LOGGER.debug("Received message without reference id and no correlation is set/matched, staring new process instance with trigger '{}'", trigger);
                    metrics.messageConsumed(CONNECTOR, MESSAGE, ((io.automatiko.engine.workflow.AbstractProcess<?>)process).process());
                    try {
                    	ProcessInstance<$Type$> pi = process.createInstance(correlation, model);
                    
                    	pi.start(trigger, null, eventData);
                    } catch (ProcessInstanceDuplicatedException e) {
                        if (allowsSignal()) {
                        	ProcessInstance<$Type$> pi = process.instances().findById(correlation, io.automatiko.engine.api.workflow.ProcessInstanceReadMode.MUTABLE_WITH_LOCK).get();
                        	pi.send(Sig.of(trigger, eventData));
                        }
                    }
                } else {
                    metrics.messageMissed(CONNECTOR, MESSAGE, ((io.automatiko.engine.workflow.AbstractProcess<?>)process).process());
                    LOGGER.warn("Received message without reference id and no correlation is set/matched, for trigger not capable of starting new instance '{}'", trigger);
                }
                
                return null;
            });
            
            
            return msg.ack();
        } catch (Exception e) {
            metrics.messageFailed(CONNECTOR, MESSAGE, ((io.automatiko.engine.workflow.AbstractProcess<?>)process).process());
        	LOGGER.error("Error when consuming message for process {}", process.id(), e);
        	return msg.nack(e);            
        } finally {
            IdentityProvider.set(null);
        }               
    }
	
	protected String correlationPayload(Object eventData, Message<?> message) {
	    
		return null;
	}
	 
	protected String correlationEvent(io.automatiko.engine.api.event.AbstractDataEvent<?> eventData, Message<?> message) {
		
		return null;
	}
	
    protected boolean acceptedPayload(Object eventData, Message<?> message) {
        return true;
    }

    protected boolean acceptedEvent(io.automatiko.engine.api.event.AbstractDataEvent<?> eventData, Message<?> message) {
        return true;
    }
	
	protected $DataType$ convert(Message<?> message, Class<?> clazz) throws Exception {
	    Object payload = message.getPayload();
	    
	    if (converter != null && !converter.isUnsatisfied()) {
	        payload = converter.get().convert(message);
	    }
	    	    
	    if (payload instanceof String) {
	        return ($DataType$) json.readValue(payload.toString(), $DataType$.class);
	    }
	    
	    return ($DataType$) payload;
	}
	
	private String correlation($DataType$ eventData, Message<?> msg) {
	    String correlation = correlationPayload(eventData, msg);
        if (correlation == null && ((io.smallrye.reactive.messaging.kafka.KafkaRecord<?, ?>) msg).getKey() != null) {
            correlation = ((io.smallrye.reactive.messaging.kafka.KafkaRecord<?, ?>) msg).getKey().toString();
        }
        return correlation;
	}
	
    private String correlation($DataEventType$ eventData, Message<?> msg) {
        String correlation = correlationEvent(eventData, msg);
        if (correlation == null && ((io.smallrye.reactive.messaging.kafka.KafkaRecord<?, ?>) msg).getKey() != null) {
            correlation = ((io.smallrye.reactive.messaging.kafka.KafkaRecord<?, ?>) msg).getKey().toString();
        }
        return correlation;
    }
	
    protected String header(Message<?> message, String name) {

        io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata<?, ?> metadata = message.getMetadata(io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata.class).orElse(null);
        if (metadata == null) {
            return null;
        }
        org.apache.kafka.common.header.Headers headers = metadata.getHeaders();
        
        org.apache.kafka.common.header.Header header = headers.lastHeader(name);
        
        if (header == null) {
            return null;
        }
        
        return new String(header.value(), StandardCharsets.UTF_8);
    }
    
    protected void cloudEventsExtensions(Message<?> message, $DataEventType$ event) {
        io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata<?, ?> metadata = message.getMetadata(io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata.class).orElse(null);
        if (metadata == null) {
            return;
        }
        org.apache.kafka.common.header.Headers headers = metadata.getHeaders();
        for (org.apache.kafka.common.header.Header header : headers.toArray()) {
            if (header.key().startsWith("ce_"))
                event.addExtension(header.key().replaceFirst("ce_", ""), new String(header.value(), StandardCharsets.UTF_8));
        }
    }
}