package com.myspace.demo;

import java.util.TimeZone;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
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
    io.automatiko.engine.service.metrics.ProcessMessagingMetrics metrics;

    public void configure() {

    }
    
	public CompletionStage<Void> consume(Message<?> msg) {
	    
	    metrics.messageReceived(CONNECTOR, MESSAGE, ((io.automatiko.engine.workflow.AbstractProcess<?>)process).process());
	    final String trigger = "$Trigger$";
        try {
            
            final $DataType$ eventData = convert(msg, $DataType$.class);
            final $Type$ model = new $Type$();
            boolean accepted = acceptedPayload(eventData, msg);
            if (!accepted) {
                metrics.messageRejected(CONNECTOR, MESSAGE, ((io.automatiko.engine.workflow.AbstractProcess<?>)process).process());
                LOGGER.debug("Message has been rejected by filter expression");
                return msg.ack();
            }
            IdentityProvider.set(new TrustedIdentityProvider("System<messaging>"));
            io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            	String correlation = correlationPayload(eventData, msg);
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
            	    metrics.messageConsumed(CONNECTOR, MESSAGE, ((io.automatiko.engine.workflow.AbstractProcess<?>)process).process());
            	    LOGGER.debug("Received message without reference id and no correlation is set/matched, staring new process instance with trigger '{}'", trigger);
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
	 
	protected String correlationEvent(Object eventData, Message<?> message) {
		
		return null;
	}
	
    protected boolean acceptedPayload(Object eventData, Message<?> message) {
        return true;
    }

    protected boolean acceptedEvent(io.automatiko.engine.api.event.AbstractDataEvent<?> eventData, Message<?> message) {
        return true;
    }
	
	protected $DataType$ convert(Message<?> message, Class<?> clazz) {
	    Object payload = message.getPayload();
	    
	    if (converter != null && !converter.isUnsatisfied()) {
	        payload = converter.get().convert(((io.smallrye.reactive.messaging.camel.CamelMessage<?>) message).getExchange().getIn());
	    }
	    
	    return ($DataType$) payload;
	}
	
	public String header(String name, Message<?> message) {
	    Object value = ((io.smallrye.reactive.messaging.camel.CamelMessage<?>) message).getExchange().getIn().getHeader(name);
	    
	    if (value != null) {
	        return value.toString();
	    }
	    
	    return null;
	}
}