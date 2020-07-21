package com.myspace.demo;

import java.util.TimeZone;
import java.util.Optional;

import io.automatik.engine.api.Application;
import io.automatik.engine.api.event.DataEvent;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.workflow.Sig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;

public class $Type$MessageConsumer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("MessageConsumer");

    Process<$Type$> process;

    Application application;
    
    Optional<Boolean> useCloudEvents = Optional.of(true);
    
    private ObjectMapper json = new ObjectMapper();

    {
        json.setDateFormat(new StdDateFormat().withColonInTimeZone(true).withTimeZone(TimeZone.getDefault()));
    }

    public void configure() {

    }
    
	public void consume(String payload) {
	    final String trigger = "$Trigger$";
        try {
            
            if (useCloudEvents.orElse(true)) {
                final $DataEventType$ eventData = json.readValue(payload, $DataEventType$.class);
        	    final $Type$ model = new $Type$();
                model.set$ModelRef$(eventData.getData());
                io.automatik.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                    
                    if (eventData.getAutomatikReferenceId() != null) {
                        LOGGER.debug("Received message with reference id '{}' going to use it to send signal '{}'", eventData.getAutomatikReferenceId(), trigger);
                        process.instances().findById(eventData.getAutomatikReferenceId()).ifPresent(pi -> pi.send(Sig.of("Message-"+trigger, eventData.getData(), eventData.getAutomatikProcessinstanceId())));
                    } else {  
                        LOGGER.debug("Received message without reference id, staring new process instance with trigger '{}'", trigger);
                        ProcessInstance<$Type$> pi = process.createInstance(model);
                        
                        if (eventData.getAutomatikStartFromNode() != null) {
                            pi.startFrom(eventData.getAutomatikStartFromNode(), eventData.getAutomatikProcessinstanceId());
                        } else {
                            pi.start(trigger, eventData.getAutomatikProcessinstanceId());
                        }
                    }
                    return null;
                });
            } else {
                final $DataType$ eventData = json.readValue(payload, $DataType$.class);
                final $Type$ model = new $Type$();
                model.set$ModelRef$(eventData);
                io.automatik.engine.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
                                    
                    LOGGER.debug("Received message without reference id, staring new process instance with trigger '{}'", trigger);
                    ProcessInstance<$Type$> pi = process.createInstance(model);
                    pi.start(trigger, null);  
                    
                    return null;
                });
            }
        } catch (Exception e) {
            LOGGER.error("Error when consuming message for process {}", process.id(), e);
        }
    }
	    
}