package org.acme.home;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.event.EventPublisher;
import io.automatiko.engine.api.jobs.ProcessInstanceJobDescription;
import io.automatiko.engine.services.event.impl.CountDownProcessInstanceEventPublisher;
import io.automatiko.quarkus.tests.AutomatikoTestProfile;
import io.automatiko.quarkus.tests.jobs.TestJobService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import io.smallrye.reactive.messaging.mqtt.MqttMessage;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@QuarkusTest
@TestProfile(AutomatikoTestProfile.class)
public class VerificationTest {
 // @formatter:off
    
    @Inject 
    @Any
    InMemoryConnector connector;
    
    @Inject
    TestJobService jobService;
    
    private CountDownProcessInstanceEventPublisher execCounter = new CountDownProcessInstanceEventPublisher();
    
    @Produces
    @Singleton
    public EventPublisher publisher() {
        return execCounter;
    }
    
    @Test
    public void testProcessExecution() throws InterruptedException {
        String humidity = "{\"timestamp\":1, \"value\" : 45.0, \"location\":\"kitchen\"}";
        String temperature = "{\"timestamp\":1, \"value\" : 29.0, \"location\":\"kitchen\"}";
        
        InMemorySource<MqttMessage<byte[]>> channelT = connector.source("home-x-temperature");
        
        InMemorySource<MqttMessage<byte[]>> channelH = connector.source("home-x-humidity");
        
        execCounter.reset(2);
        channelT.send(MqttMessage.of("home/kitchen/temperature", temperature.getBytes()));
        channelH.send(MqttMessage.of("home/kitchen/humidity", humidity.getBytes()));
        execCounter.waitTillCompletion(5);
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/climate")
        .then().statusCode(200)
            .body("$.size()", is(1));
        
        Map data = given()
            .accept(ContentType.JSON)
        .when()
            .get("/climate/kitchen")
        .then()
            .statusCode(200).body("id", is("kitchen")).extract().as(Map.class);
        
        List<?> tempBucket = (List<?>) data.get("temperatureBucket");
        List<?> humidityBucket = (List<?>) data.get("humidityBucket");
        
        assertEquals(1, tempBucket.size());
        assertEquals(1, humidityBucket.size());
        
        // let's push data for living room
        humidity = "{\"timestamp\":1, \"value\" : 45.0, \"location\":\"livingroom\"}";
        temperature = "{\"timestamp\":1, \"value\" : 29.0, \"location\":\"livingroom\"}";
        
        execCounter.reset(2);
        channelT.send(MqttMessage.of("home/livingroom/temperature", temperature.getBytes()));
        channelH.send(MqttMessage.of("home/livingroom/humidity", humidity.getBytes()));
        execCounter.waitTillCompletion(5);
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/climate")
        .then().statusCode(200)
            .body("$.size()", is(2));
        
        data = given()
            .accept(ContentType.JSON)
        .when()
            .get("/climate/livingroom")
        .then()
            .statusCode(200).body("id", is("livingroom")).extract().as(Map.class);
        
        tempBucket = (List<?>) data.get("temperatureBucket");
        humidityBucket = (List<?>) data.get("humidityBucket");
        
        assertEquals(1, tempBucket.size());
        assertEquals(1, humidityBucket.size());

        // abort instance for kitchen
        given()
            .accept(ContentType.JSON)
        .when()
            .delete("/climate/kitchen")
        .then()
            .statusCode(200);
        
        // abort instance for livingroom
        given()
            .accept(ContentType.JSON)
        .when()
            .delete("/climate/livingroom")
        .then()
            .statusCode(200);
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/climate")
        .then().statusCode(200)
            .body("$.size()", is(0));
    }
    
    @Test
    public void testProcessExecutionWithTimer() throws InterruptedException {
        String humidity = "{\"timestamp\":1, \"value\" : 45.0, \"location\":\"kitchen\"}";
        String temperature = "{\"timestamp\":1, \"value\" : 29.0, \"location\":\"kitchen\"}";
        
        InMemorySource<MqttMessage<byte[]>> channelT = connector.source("home-x-temperature");
        
        InMemorySource<MqttMessage<byte[]>> channelH = connector.source("home-x-humidity");
        
        InMemorySink<byte[]> expired = connector.sink("expired");
        
        execCounter.reset(2);
        channelT.send(MqttMessage.of("home/kitchen/temperature", temperature.getBytes()));
        channelH.send(MqttMessage.of("home/kitchen/humidity", humidity.getBytes()));
        execCounter.waitTillCompletion(5);
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/climate")
        .then().statusCode(200)
            .body("$.size()", is(1));
        
        Map data = given()
            .accept(ContentType.JSON)
        .when()
            .get("/climate/kitchen")
        .then()
            .statusCode(200).body("id", is("kitchen")).extract().as(Map.class);
        
        List<?> tempBucket = (List<?>) data.get("temperatureBucket");
        List<?> humidityBucket = (List<?>) data.get("humidityBucket");
        
        assertEquals(1, tempBucket.size());
        assertEquals(1, humidityBucket.size());
        
        // let's push data for living room
        humidity = "{\"timestamp\":1, \"value\" : 45.0, \"location\":\"livingroom\"}";
        temperature = "{\"timestamp\":1, \"value\" : 29.0, \"location\":\"livingroom\"}";
        
        execCounter.reset(2);
        channelT.send(MqttMessage.of("home/livingroom/temperature", temperature.getBytes()));
        channelH.send(MqttMessage.of("home/livingroom/humidity", humidity.getBytes()));
        execCounter.waitTillCompletion(5);
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/climate")
        .then().statusCode(200)
            .body("$.size()", is(2));
        
        data = given()
            .accept(ContentType.JSON)
        .when()
            .get("/climate/livingroom")
        .then()
            .statusCode(200).body("id", is("livingroom")).extract().as(Map.class);
        
        tempBucket = (List<?>) data.get("temperatureBucket");
        humidityBucket = (List<?>) data.get("humidityBucket");
        
        assertEquals(1, tempBucket.size());
        assertEquals(1, humidityBucket.size());
        
        List<ProcessInstanceJobDescription> jobs = jobService.processInstanceJobs("climate");
        assertEquals(2, jobs.size());
        
        jobService.triggerProcessInstanceJob(jobs.get(0).id());
        jobService.triggerProcessInstanceJob(jobs.get(1).id());        
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/climate")
        .then().statusCode(200)
            .body("$.size()", is(0));
        
        assertEquals(2, expired.received().size());
        
        byte[] content = expired.received().get(0).getPayload();
        assertEquals("\"expired\"", new String(content));
        
        content = expired.received().get(1).getPayload();
        assertEquals("\"expired\"", new String(content));
    }
    
 // @formatter:on
}
