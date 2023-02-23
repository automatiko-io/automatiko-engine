package com.myspace.demo;


import java.util.Optional;
import java.util.TimeZone;

import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.api.runtime.process.ProcessInstance;
import io.automatiko.engine.workflow.process.instance.WorkflowProcessInstance;
import io.automatiko.engine.api.event.DataEvent;
import io.automatiko.engine.api.event.EventSource;



@SuppressWarnings({ "rawtypes", "unchecked" })
public class MessageProducer {
    
    jakarta.enterprise.inject.Instance<io.automatiko.engine.api.io.OutputConverter<$Type$, Object>> converter;    
    
    @jakarta.inject.Inject
    EventSource eventSource;
    
    public void produce(ProcessInstance pi, NodeInstance nodeInstance, $Type$ eventData) {
        
        eventSource.produce("$destination$", "$sourcePrefix$" + "/" + id(pi), marshall(pi, eventData), subject(pi));
    }
        
    private Object marshall(ProcessInstance pi, $Type$ eventData) {
        try {
            Object payload = eventData;                
            if (converter != null && !converter.isUnsatisfied()) {
                payload = converter.get().convert(eventData);                
            }
            return payload;
        
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    protected String subject(ProcessInstance pi) {
        return null;
    }
    
    public String id(ProcessInstance pi) {
        if (pi.getParentProcessInstanceId() != null) {
            return pi.getParentProcessInstanceId() + ":" + pi.getId();
        } else {
            return pi.getId();
        }
    }
}