package org.automatiko.funq;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;

import org.automatiko.funq.MockEventSource.EventData;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class GreetingsTest {

    @Inject
    MockEventSource eventSource;

    // @formatter:off
    @Test
    public void testStartFunctionEndpoint() {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header("ce-id", UUID.randomUUID().toString())
            .header("ce-type", "com.acme.sayhello")
            .header("ce-source", "test")
            .header("ce-specversion", "1.0")
            .body("{\"name\" : \"john\"}")
        .when()
            .post("/")
        .then()
            .statusCode(204);
        
        List<EventData> events = eventSource.events();
        assertEquals(2, events.size());
        
        EventData data = events.get(0);
        assertEquals("org.acme.travels.greetings", data.source.split("/")[0]);
        assertEquals("org.acme.travels.greetings.updatemessage", data.type);
        
        data = events.get(1);
        assertEquals("org.acme.travels.greetings", data.source.split("/")[0]);
        assertEquals("custom", data.type);
        
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header("ce-id", UUID.randomUUID().toString())
            .header("ce-type", "org.acme.travels.greetings.updatemessage")
            .header("ce-source", "test")
            .header("ce-specversion", "1.0")
            .body("{\"name\" : \"john\"}")
        .when()
            .post("/")
        .then()
            .statusCode(204);
        
        events = eventSource.events();
        assertEquals(1, events.size());
        
        data = events.get(0);
        assertEquals("org.acme.travels.greetings.updatemessage", data.source.split("/")[0]);
        assertEquals("org.acme.travels.greetings.end", data.type);       
    }
    
    @Test
    public void testStartFunctionEndpointOtherPath() {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header("ce-id", UUID.randomUUID().toString())
            .header("ce-type", "com.acme.sayhello")
            .header("ce-source", "test")
            .header("ce-specversion", "1.0")
            .body("{\"name\" : \"mary\"}")
        .when()
            .post("/")
        .then()
            .statusCode(204);
        List<EventData> events = eventSource.events();
        assertEquals(2, events.size());
        
        EventData data = events.get(0);
        assertEquals("org.acme.travels.greetings", data.source.split("/")[0]);
        assertEquals("org.acme.travels.greetings.spanishname", data.type);
        
        data = events.get(1);
        assertEquals("org.acme.travels.greetings", data.source.split("/")[0]);
        assertEquals("custom", data.type);
        
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header("ce-id", UUID.randomUUID().toString())
            .header("ce-type", "org.acme.travels.greetings.spanishname")
            .header("ce-source", "test")
            .header("ce-specversion", "1.0")
            .body("{\"name\" : \"mary\"}")
        .when()
            .post("/")
        .then()
            .statusCode(204);
        events = eventSource.events();
        assertEquals(1, events.size());
        
        data = events.get(0);
        assertEquals("org.acme.travels.greetings.spanishname", data.source.split("/")[0]);
        assertEquals("org.acme.travels.greetings.endevent2", data.type); 
    }
    
    @Test
    public void testStartFunctionEndpointOtherPathStructured() {
        given()
            .contentType("application/cloudevents+json")
            .accept("application/cloudevents+json")
            .body("{\n"
                    + "  \"id\" : \"" + UUID.randomUUID().toString() + "\",\n"
                    + "  \"type\" : \"com.acme.sayhello\",\n"
                    + "  \"source\": \"test\",\n"
                    + "  \"specversion\": \"1.0\",\n"
                    + "  \"datacontenttype\": \"application/json\",\n"
                    + "  \"data\": {\n"
                    + "    \"name\" : \"mary\"\n"
                    + "  }\n"
                    + "}")
        .when()
            .post("/")
        .then()
            .statusCode(204);
        List<EventData> events = eventSource.events();
        assertEquals(2, events.size());
        
        EventData data = events.get(0);
        assertEquals("org.acme.travels.greetings", data.source.split("/")[0]);
        assertEquals("org.acme.travels.greetings.spanishname", data.type);
        
        data = events.get(1);
        assertEquals("org.acme.travels.greetings", data.source.split("/")[0]);
        assertEquals("custom", data.type);
          
        given()
            .contentType("application/cloudevents+json")
            .accept("application/cloudevents+json")
            .body("{\n"
                    + "  \"id\" : \"" + UUID.randomUUID().toString() + "\",\n"
                    + "  \"type\" : \"org.acme.travels.greetings.spanishname\",\n"
                    + "  \"source\": \"test\",\n"
                    + "  \"specversion\": \"1.0\",\n"
                    + "  \"datacontenttype\": \"application/json\",\n"
                    + "  \"data\": {\n"
                    + "    \"name\" : \"mary\"\n"
                    + "  }\n"
                    + "}")
        .when()
            .post("/")
        .then()
            .statusCode(204);
        events = eventSource.events();
        assertEquals(1, events.size());
        
        data = events.get(0);
        assertEquals("org.acme.travels.greetings.spanishname", data.source.split("/")[0]);
        assertEquals("org.acme.travels.greetings.endevent2", data.type); 
    }
    
    @Test
    public void testStartSingleFunctionEndpoint() {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header("ce-id", UUID.randomUUID().toString())
            .header("ce-type", "com.acme.sayhellosingle")
            .header("ce-source", "test")
            .header("ce-specversion", "1.0")
            .body("{\"name\" : \"john\"}")
        .when()
            .post("/")
        .then()
            .statusCode(204);
        
        List<EventData> events = eventSource.events();
        assertEquals(1, events.size());
        
        EventData data = events.get(0);
        assertEquals("org.acme.travels.greetingssingle", data.source.split("/")[0]);
        assertEquals("org.acme.travels.greetingssingle.end", data.type);       
    }
    // @formatter:on
}