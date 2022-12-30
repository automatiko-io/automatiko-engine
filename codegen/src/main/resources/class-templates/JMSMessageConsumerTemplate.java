package com.myspace.demo;

import java.util.TimeZone;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.Collection;
import java.util.Enumeration;
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
    
    javax.enterprise.inject.Instance<io.automatiko.engine.api.io.InputConverter<$DataType$>> converter;
  
    @javax.inject.Inject
    ObjectMapper json;
    
    @javax.inject.Inject
    io.automatiko.engine.service.metrics.ProcessMessagingMetrics metrics;

    public void configure() {

    }
    
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
                String contentType = appProperty(msg, "contentType");
                model = new $Type$(); 
                if (contentType != null && contentType.startsWith("application/cloudevents+json")) {
                    // structured
                    event = json.readValue(msg.getPayload(), $DataEventType$.class);
                    eventData = event.getData();
                    
                } else {
                    // binary
                    eventData = convert(msg, $DataType$.class);
                    event =  new $DataEventType$(appProperty(msg, "ce_specversion"), appProperty(msg, "ce_id"), appProperty(msg, "ce_source"), appProperty(msg, "ce_type"), appProperty(msg, "ce_subject"), appProperty(msg, "ce_time"), eventData);
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
	
   protected String appProperty(Message<String> message, String name) {

       io.smallrye.reactive.messaging.jms.IncomingJmsMessageMetadata metadata = message.getMetadata(io.smallrye.reactive.messaging.jms.IncomingJmsMessageMetadata.class).orElse(null);
        if (metadata == null || metadata.getProperties() == null) {
            return null;
        }
        return metadata.getProperties().getStringProperty(name);        
    }
   
   protected void cloudEventsExtensions(Message<String> message, $DataEventType$ event) {
       io.smallrye.reactive.messaging.jms.IncomingJmsMessageMetadata metadata = message.getMetadata(io.smallrye.reactive.messaging.jms.IncomingJmsMessageMetadata.class).orElse(null);
       if (metadata == null || metadata.getProperties() == null) {
           return;
       }
       Enumeration<String> it = metadata.getProperties().getPropertyNames();
       while (it.hasMoreElements()) {
           String name = it.nextElement();
           if (name.startsWith("ce_"))
           event.addExtension(name.replaceFirst("ce_", ""), metadata.getProperties().getStringProperty(name));
       }
   }
}