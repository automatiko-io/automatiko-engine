package com.myspace.demo;


import java.util.Optional;
import java.util.TimeZone;

import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.api.runtime.process.ProcessInstance;
import io.automatiko.engine.api.event.DataEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;

import org.eclipse.microprofile.reactive.messaging.Message;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class MessageProducer {
    
    Object emitter;
    
    Optional<Boolean> useCloudEvents = Optional.of(true);
    private ObjectMapper json = new ObjectMapper();

	{
		json.setDateFormat(new StdDateFormat().withColonInTimeZone(true).withTimeZone(TimeZone.getDefault()));
	}

    public void configure() {
		
    }
    
	public void produce(ProcessInstance pi, NodeInstance nodeInstance, $Type$ eventData) {
               
    }
	    
	private Message<?> marshall(ProcessInstance pi, $Type$ eventData) {
	    try {
	        
	        if (useCloudEvents.orElse(true)) {
        	    $DataEventType$ event = new $DataEventType$("", "", eventData);
        	    return Message.of(json.writeValueAsBytes(event));
	        } else {
	            return Message.of(json.writeValueAsBytes(eventData));
	        }
	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }
	}
}