package com.sample.extras;

import javax.inject.Inject;

import com.sample.Ticket;

import io.automatiko.engine.api.event.EventSource;
import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventMapping;

public class ReplyFunctions {

    @Inject
    EventSource eventSource;

    @Funq
    @CloudEventMapping(trigger = "com.tickets.generated")
    public void toAck(CloudEvent<Ticket> event) {
        System.out.println("Ticket " + event.data() + " to be acked " + event.subject());
        Ticket ticket = event.data();
        ticket.setType("ACKed");

        String workFlowInstanceId = event.source().split("/")[1];

        eventSource.produce("com.sample.message.messages.waitforack", "com.tickets.generated/12345", ticket,
                workFlowInstanceId);

    }

    @Funq
    @CloudEventMapping(trigger = "com.tickets.approved")
    public void approved(CloudEvent<Ticket> event) {
        System.out.println("Ticket " + event.data() + " approved " + event.subject() + " source " + event.source());
    }
}
