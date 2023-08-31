package com.myspace.demo;

import java.util.TimeZone;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.Collection;
import java.util.Optional;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.auth.TrustedIdentityProvider;
import io.automatiko.engine.api.event.AbstractDataEvent;
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
	public CompletionStage<Void> consume(Message<String> msg) {
	    metrics.messageReceived(CONNECTOR, MESSAGE, ((io.automatiko.engine.workflow.AbstractProcess<?>)process).process());
	    final String trigger = "$Trigger$";
        try {
            IdentityProvider.set(new TrustedIdentityProvider("System<messaging>"));

            final $DataType$ eventData;
            final $Type$ model;   
            final String correlation;
            LOGGER.debug("Received message with payload '{}'", msg.getPayload());
            boolean accepted;
            if (useCloudEvents.orElse(false)) {
                $DataEventType$ event;
                String contentType = contentType(msg);
                model = new $Type$(); 
                if (contentType.startsWith("application/cloudevents+json")) {
                    // structured
                    event = json.readValue(msg.getPayload(), $DataEventType$.class);
                    eventData = event.getData();
                    
                } else {
                    // binary
                    eventData = convert(msg, $DataType$.class);
                    event =  new $DataEventType$(appProperty(msg, "cloudEvents:specversion"), appProperty(msg, "cloudEvents:id"), appProperty(msg, "cloudEvents:source"), appProperty(msg, "cloudEvents:type"), appProperty(msg, "cloudEvents:subject"), appProperty(msg, "cloudEvents:time"), eventData);
                    
                    cloudEventsExtensions(msg, event);
                }
                
                correlation = correlationEvent(event, msg);
                accepted = acceptedEvent(event, msg);
            } else {
                eventData = convert(msg, $DataType$.class);
                model = new $Type$();  
                
                correlation = correlationPayload(eventData, msg); 
                
                accepted = acceptedPayload(eventData, msg);
            }      
            if (!accepted) {
                metrics.messageRejected(CONNECTOR, MESSAGE, ((io.automatiko.engine.workflow.AbstractProcess<?>)process).process());
                LOGGER.debug("Message has been rejected by filter expression");
                return msg.ack();
            }
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
                    	ProcessInstance<$Type$> pi = process.instances().findById(correlation, io.automatiko.engine.api.workflow.ProcessInstanceReadMode.MUTABLE_WITH_LOCK).get();
                    	pi.send(Sig.of(trigger, eventData));
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
        }                
    }
	
	protected String correlationPayload(Object eventData, Message<String> message) {
		
		return null;
	}
	 
	protected String correlationEvent(io.automatiko.engine.api.event.AbstractDataEvent<?> eventData, Message<String> message) {
		
		return null;
	}
	
    protected boolean acceptedPayload(Object eventData, Message<String> message) {
        return true;
    }

    protected boolean acceptedEvent(io.automatiko.engine.api.event.AbstractDataEvent<?> eventData, Message<String> message) {
        return true;
    }
	

	protected $DataType$ convert(Message<String> message, Class<?> clazz) throws Exception {
        
        if (converter != null && !converter.isUnsatisfied()) {
            return converter.get().convert(message);
        }
        
        return ($DataType$) json.readValue(message.getPayload(), $DataType$.class);
    }
	
   protected String contentType(Message<String> message) {

       io.smallrye.reactive.messaging.amqp.IncomingAmqpMetadata metadata = message.getMetadata(io.smallrye.reactive.messaging.amqp.IncomingAmqpMetadata.class).orElse(null);
        if (metadata == null) {
            return "application/json; charset=UTF-8";
        }
        return metadata.getContentType();        
    }
	
   protected String appProperty(Message<String> message, String name) {

       io.smallrye.reactive.messaging.amqp.IncomingAmqpMetadata metadata = message.getMetadata(io.smallrye.reactive.messaging.amqp.IncomingAmqpMetadata.class).orElse(null);
        if (metadata == null || metadata.getProperties() == null) {
            return null;
        }
        return metadata.getProperties().getString(name);        
    }
   
   protected void cloudEventsExtensions(Message<String> message, $DataEventType$ event) {

       io.smallrye.reactive.messaging.amqp.IncomingAmqpMetadata metadata = message.getMetadata(io.smallrye.reactive.messaging.amqp.IncomingAmqpMetadata.class).orElse(null);
        if (metadata == null || metadata.getProperties() == null) {
            return;
        }
        for (String name : metadata.getProperties().fieldNames()) {
            if (name.startsWith("cloudEvents:"))
            event.addExtension(name.replaceFirst("cloudEvents:", ""), metadata.getProperties().getString(name));
        }
    }
}