package com.myspace.demo;

import java.util.TimeZone;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
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
  
    
    private ObjectMapper json = new ObjectMapper();

    {
        json.setDateFormat(new StdDateFormat().withColonInTimeZone(true).withTimeZone(TimeZone.getDefault()));
    }

    public void configure() {

    }
    
	public CompletionStage<Void> consume(Message<byte[]> msg) {
	    final String trigger = "$Trigger$";
        try {
            IdentityProvider.set(new TrustedIdentityProvider("System<messaging>"));
            if (useCloudEvents.orElse(true)) {
                final $DataEventType$ eventData = json.readValue(msg.getPayload(), $DataEventType$.class);
                final $Type$ model = new $Type$();   
                io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                    
               
                    	String correlation = correlationEvent(eventData, msg);
                    	if (correlation != null) {
                    		LOGGER.debug("Correlation ({}) is set, attempting to find if there is matching instance already active", correlation);
                    		Optional possiblyFound = process.instances().findById(correlation, io.automatiko.engine.api.workflow.ProcessInstanceReadMode.MUTABLE_WITH_LOCK);
                    		if (possiblyFound.isPresent()) {
                    			ProcessInstance pInstance = (ProcessInstance) possiblyFound.get();
                    			LOGGER.debug("Found process instance {} matching correlation {}, signaling instead of starting new instance", pInstance.id(), correlation);
                    			pInstance.send(Sig.of(trigger, eventData.getData()));
                    			return null;
                    		}
                    		
                        } 
                        LOGGER.debug("Received message without reference id and no correlation is set/matched, staring new process instance with trigger '{}'", trigger);                        
                        
                    	try {
                    		ProcessInstance<$Type$> pi = process.createInstance(correlation, model);
                    		pi.start(trigger, null, eventData.getData());
                    	} catch (ProcessInstanceDuplicatedException e) {
                        	ProcessInstance<$Type$> pi = process.instances().findById(correlation, io.automatiko.engine.api.workflow.ProcessInstanceReadMode.MUTABLE_WITH_LOCK).get();
                        	pi.send(Sig.of(trigger, eventData.getData()));
                        }
                        
                    
                    return null;
                });                
            } else {
                final $DataType$ eventData = json.readValue(msg.getPayload(), $DataType$.class);
                final $Type$ model = new $Type$();                
                io.automatiko.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                	String correlation = correlationPayload(eventData, msg);
                	if (correlation != null) {
                		LOGGER.debug("Correlation ({}) is set, attempting to find if there is matching instance already active", correlation);
                		Optional possiblyFound = process.instances().findById(correlation, io.automatiko.engine.api.workflow.ProcessInstanceReadMode.MUTABLE_WITH_LOCK);
                		if (possiblyFound.isPresent()) {
                			ProcessInstance pInstance = (ProcessInstance) possiblyFound.get();
                			LOGGER.debug("Found process instance {} matching correlation {}, signaling instead of starting new instance", pInstance.id(), correlation);
                			pInstance.send(Sig.of(trigger, eventData));
                			return null;
                		}
                		
                    } 
                    LOGGER.debug("Received message without reference id and no correlation is set/matched, staring new process instance with trigger '{}'", trigger);
                    try {
                    	ProcessInstance<$Type$> pi = process.createInstance(correlation, model);
                    	pi.start(trigger, null, eventData);
                    } catch (ProcessInstanceDuplicatedException e) {
                    	ProcessInstance<$Type$> pi = process.instances().findById(correlation, io.automatiko.engine.api.workflow.ProcessInstanceReadMode.MUTABLE_WITH_LOCK).get();
                    	pi.send(Sig.of(trigger, eventData));
                    }
                    
                    return null;
                });
            }
            
            return msg.ack();
        } catch (Exception e) {
        	LOGGER.error("Error when consuming message for process {}", process.id(), e);
        	return msg.nack(e);            
        } finally {
            IdentityProvider.set(null);
        }               
    }
	
	protected String correlationPayload(Object eventData, Message<byte[]> message) {
		
		return null;
	}
	 
	protected String correlationEvent(Object eventData, Message<byte[]> message) {
		
		return null;
	}
}