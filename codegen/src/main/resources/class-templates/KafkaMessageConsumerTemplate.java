package com.myspace.demo;

import java.util.TimeZone;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.Collection;
import java.util.Optional;

import io.automatik.engine.api.Application;
import io.automatik.engine.api.auth.IdentityProvider;
import io.automatik.engine.api.auth.TrustedIdentityProvider;
import io.automatik.engine.api.event.DataEvent;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.api.workflow.ProcessInstanceDuplicatedException;
import io.automatik.engine.workflow.Sig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;

import org.eclipse.microprofile.reactive.messaging.Message;

public class $Type$MessageConsumer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("MessageConsumer");

    Process<$Type$> process;

    Application application;
    
    Optional<Boolean> useCloudEvents = Optional.of(false);
    
    javax.enterprise.inject.Instance<io.automatik.engine.api.io.InputConverter<$DataType$>> converter;
    
    @javax.inject.Inject
    ObjectMapper json;

    public void configure() {

    }
    
	public CompletionStage<Void> consume(Message<?> msg) {
	    final String trigger = "$Trigger$";
        try {
            io.smallrye.reactive.messaging.kafka.KafkaRecord<?, ?> record = (io.smallrye.reactive.messaging.kafka.KafkaRecord<?, ?>) msg;
            
            final $DataType$ eventData = convert(record, $DataType$.class);
            final $Type$ model = new $Type$();  
            IdentityProvider.set(new TrustedIdentityProvider("System<messaging>"));
            io.automatik.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                String correlation;
                if (record.getKey() != null) {
                    correlation = ((io.smallrye.reactive.messaging.kafka.KafkaRecord<?, ?>) msg).getKey().toString();
                } else {
                    correlation = correlationPayload(eventData, msg);
                }
                if (canStartInstance()) {
                    LOGGER.debug("Received message without reference id and no correlation is set/matched, staring new process instance with trigger '{}'", trigger);
                    try {
                        ProcessInstance<$Type$> pi = process.createInstance(correlation, model);
                    
                        pi.start(trigger, null, eventData);
                    } catch (ProcessInstanceDuplicatedException e) {
//                        ProcessInstance<$Type$> pi = process.instances().findById(correlation).get();
//                        pi.send(Sig.of(trigger, eventData));
                    }
                } else {
                 
                	if (correlation != null) {
                		LOGGER.debug("Correlation ({}) is set, attempting to find if there is matching instance already active", correlation);
                		Collection possiblyFound = process.instances().findByIdOrTag(io.automatik.engine.api.workflow.ProcessInstanceReadMode.MUTABLE, correlation);
                        if (!possiblyFound.isEmpty()) {
                            
                            possiblyFound.forEach(pi -> {
                                ProcessInstance pInstance = (ProcessInstance) pi;
                                LOGGER.debug("Found process instance {} matching correlation {}, signaling instead of starting new instance", pInstance.id(), correlation);
                                pInstance.send(Sig.of(canStartInstance() ? trigger : "Message-" + trigger, eventData));
                            });
                            return null;
                        }
            		
                    } else {   
                        LOGGER.warn("Received message without reference id and no correlation is set/matched, for trigger not capable of starting new instance '{}'", trigger);
                    }
                }

                
                return null;
            });
            
            
            return msg.ack();
        } catch (Exception e) {
        	LOGGER.error("Error when consuming message for process {}", process.id(), e);
        	return msg.nack(e);            
        }                
    }
	
	protected String correlationPayload(Object eventData, Message<?> message) {
	    
		return null;
	}
	 
	protected String correlationEvent(Object eventData, Message<?> message) {
		
		return null;
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
}