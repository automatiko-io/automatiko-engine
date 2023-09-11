package org.acme.home;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.event.EventPublisher;
import io.automatiko.engine.services.event.impl.CountDownProcessInstanceEventPublisher;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import io.smallrye.reactive.messaging.pulsar.PulsarMessage;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@QuarkusTest
@TestProfile(BinaryCloudEventTestProfile.class)
public class BinaryCEVerificationTest {
 // @formatter:off
    
    @Inject 
    @Any
    InMemoryConnector connector;
    
    private CountDownProcessInstanceEventPublisher execCounter = new CountDownProcessInstanceEventPublisher();
    
    @Produces
    @Singleton
    public EventPublisher publisher() {
        return execCounter;
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testProcessExecution() throws InterruptedException {
        String temp = "{\"timestamp\":1, \"value\" : 45.0, \"location\":\"kitchen\"}";
        
        InMemorySource<PulsarMessage<String>> channelAlarms = connector.source("alarm");  
        InMemorySink<PulsarMessage<String>> channelPocessed = connector.sink("processed");
        
        String id = "room";
        
        execCounter.reset(1);
        channelAlarms.send(PulsarMessage.of(temp, id));
        execCounter.waitTillCompletion(5);        
        
        Map data = given()
            .accept(ContentType.JSON)
        .when()
            .get("/alarms/" + id)
        .then()
            .statusCode(200).body("id", is(id)).extract().as(Map.class);
        
        Object alarm = data.get("alarm");        
        
        assertNotNull(alarm);
        List<Map<String, String>> taskInfo = given()
                .accept(ContentType.JSON)
            .when()
                .get("/alarms/" + id + "/tasks")
            .then()
                .statusCode(200)
                .extract().as(List.class);

        assertEquals(1, taskInfo.size());
        
        String taskId = taskInfo.get(0).get("id");
        String taskName = taskInfo.get(0).get("name");
        
        assertEquals("Task_Name", taskName);
        
        String payload = "{}";
        given().
            contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(payload)
        .when()
            .post("/alarms/" + id + "/" + taskName + "/" + taskId)
        .then()
            .statusCode(200).body("id", is(id), "alarm", notNullValue());
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/alarms")
        .then().statusCode(200)
            .body("$.size()", is(0));
        
        List<? extends Message<PulsarMessage<String>>> received = channelPocessed.received();
        assertEquals(1, received.size());
        channelPocessed.clear();
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testProcessViaIntermediateEventExecution() throws InterruptedException {
        String temp = "{\"timestamp\":1, \"value\" : 25.0, \"location\":\"kitchen\"}";
        
        InMemorySource<PulsarMessage<String>> channelAlarms = connector.source("alarm");
        
        InMemorySource<PulsarMessage<String>> channelExtraAlarms = connector.source("extraalarms");        
        
        InMemorySink<PulsarMessage<String>> channelPocessed = connector.sink("processed");
        
        String id = "room";
        
        execCounter.reset(1);
        channelAlarms.send(PulsarMessage.of(temp, id));
        execCounter.waitTillCompletion(5);
        
        Map data = given()
            .accept(ContentType.JSON)
        .when()
            .get("/alarms/" + id)
        .then()
            .statusCode(200).body("id", is(id)).extract().as(Map.class);
        
        Object alarm = data.get("alarm");        
        
        assertNotNull(alarm);
        List<Map<String, String>> taskInfo = given()
                .accept(ContentType.JSON)
            .when()
                .get("/alarms/" + id + "/tasks")
            .then()
                .statusCode(200)
                .extract().as(List.class);

        assertEquals(0, taskInfo.size());
        
        execCounter.reset(1);
        channelExtraAlarms.send(PulsarMessage.of(temp, id));
        execCounter.waitTillCompletion(5);
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/alarms")
        .then().statusCode(200)
            .body("$.size()", is(0));
        
        List<? extends Message<PulsarMessage<String>>> received = channelPocessed.received();
        assertEquals(0, received.size());
        channelPocessed.clear();
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testProcessViaBoundaryEventExecution() throws InterruptedException {
        String temp = "{\"timestamp\":1, \"value\" : 45.0, \"location\":\"kitchen\"}";
        
        InMemorySource<PulsarMessage<String>> channelAlarms = connector.source("alarm");        
        InMemorySource<PulsarMessage<String>> channelCanceledAlarms = connector.source("canceledalarms");               
        InMemorySink<PulsarMessage<String>> channelPocessed = connector.sink("processed");
        
        String id = "room";
        
        execCounter.reset(1);
        channelAlarms.send(PulsarMessage.of(temp, id));
        execCounter.waitTillCompletion(5);
        
        Map data = given()
            .accept(ContentType.JSON)
        .when()
            .get("/alarms/" + id)
        .then()
            .statusCode(200).body("id", is(id)).extract().as(Map.class);
        
        Object alarm = data.get("alarm");        
        
        assertNotNull(alarm);
        List<Map<String, String>> taskInfo = given()
                .accept(ContentType.JSON)
            .when()
                .get("/alarms/" + id + "/tasks")
            .then()
                .statusCode(200)
                .extract().as(List.class);

        assertEquals(1, taskInfo.size());
        
        execCounter.reset(1);
        channelCanceledAlarms.send(PulsarMessage.of(temp, id));
        execCounter.waitTillCompletion(5);
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/alarms")
        .then().statusCode(200)
            .body("$.size()", is(0));
        
        List<? extends Message<PulsarMessage<String>>> received = channelPocessed.received();
        assertEquals(0, received.size());
        channelPocessed.clear();
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testProcessViaIntermediateEventExecutionCorrelation() throws InterruptedException {
        String temp = "{\"timestamp\":1, \"value\" : 25.0, \"location\":\"kitchen\"}";
        
        InMemorySource<PulsarMessage<String>> channelAlarms = connector.source("alarm");
        
        InMemorySource<PulsarMessage<String>> channelExtraAlarms = connector.source("extraalarms");        
        
        InMemorySink<PulsarMessage<String>> channelPocessed = connector.sink("processed");
        
        String id = "room";
        String id2 = "room2";
        
        execCounter.reset(2);
        channelAlarms.send(PulsarMessage.of(temp, id));
        Thread.sleep(2000);
        channelAlarms.send(PulsarMessage.of(temp, id2));
        execCounter.waitTillCompletion(5);        
        
        Map data = given()
            .accept(ContentType.JSON)
        .when()
            .get("/alarms/" + id)
        .then()
            .statusCode(200).body("id", is(id)).extract().as(Map.class);
        
        Object alarm = data.get("alarm");        
        
        assertNotNull(alarm);
        List<Map<String, String>> taskInfo = given()
                .accept(ContentType.JSON)
            .when()
                .get("/alarms/" + id + "/tasks")
            .then()
                .statusCode(200)
                .extract().as(List.class);

        assertEquals(0, taskInfo.size());
        
        execCounter.reset(1);
        channelExtraAlarms.send(PulsarMessage.of(temp, id));
        execCounter.waitTillCompletion(5);
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/alarms")
        .then().statusCode(200)
            .body("$.size()", is(1));
        
        List<? extends Message<PulsarMessage<String>>> received = channelPocessed.received();
        assertEquals(0, received.size());
        channelPocessed.clear();
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/alarms/" + id)
        .then()
            .statusCode(404);
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/alarms/" + id2)
        .then()
            .statusCode(200);
        
        given()
            .accept(ContentType.JSON)
        .when()
            .delete("/alarms/" + id2)
        .then()
            .statusCode(200);
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/alarms")
        .then().statusCode(200)
            .body("$.size()", is(0));        
    }
 // @formatter:on
}
