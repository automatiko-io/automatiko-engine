package org.automatiko.funq;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.automatiko.funq.MockEventSource.EventData;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class MessagesTest {

    @Inject
    MockEventSource eventSource;

    // @formatter:off
    @Test
    public void testStartFunctionEndpoint() {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header("ce-id", UUID.randomUUID().toString())
            .header("ce-type", "com.sample.message.messages")
            .header("ce-source", "test")
            .header("ce-specversion", "1.0")
            .body("{\n"
                    + "  \"ticket\" : {\n"
                    + "    \"id\" : \"\",\n"
                    + "    \"type\" : \"new\",\n"
                    + "    \"owner\" : \"john\"\n"
                    + "  }\n"
                    + "}")
        .when()
            .post("/")
        .then()
            .statusCode(204);
        
        List<EventData> events = eventSource.events();
        assertEquals(1, events.size());
        
        EventData data = events.get(0);
        assertEquals("com.sample.message.messages.SendTicket", data.source.split("/")[0]);
        assertEquals("com.tickets.generated", data.type);
        
        String workflowInstanceId = data.source.split("/")[1];
        
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header("ce-id", UUID.randomUUID().toString())
            .header("ce-type", "com.sample.message.messages.waitforack")
            .header("ce-source", "test/1234")
            .header("ce-subject", workflowInstanceId)
            .header("ce-specversion", "1.0")
            .body("{\n"
                    + "  \"id\" : \"\",\n"
                    + "  \"type\" : \"acked\",\n"
                    + "  \"owner\" : \"john\"\n"
                    + "}")
        .when()
            .post("/")
        .then()
            .statusCode(204);
        
        events = eventSource.events();
        assertEquals(1, events.size());
        
        data = events.get(0);
        assertEquals("com.sample.message.messages.approved", data.source.split("/")[0]);
        assertEquals("com.tickets.approved", data.type);

    }
    
    @Test
    public void testStartFunctionEndpointCorrelationBased() {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header("ce-id", UUID.randomUUID().toString())
            .header("ce-type", "com.sample.message.messages")
            .header("ce-source", "test")
            .header("ce-specversion", "1.0")
            .body("{\n"
                    + "  \"ticket\" : {\n"
                    + "    \"id\" : \"\",\n"
                    + "    \"type\" : \"new\",\n"
                    + "    \"owner\" : \"john\"\n"
                    + "  }\n"
                    + "}")
        .when()
            .post("/")
        .then()
            .statusCode(204);
        
        List<EventData> events = eventSource.events();
        assertEquals(1, events.size());
        
        EventData data = events.get(0);
        assertEquals("com.sample.message.messages.SendTicket", data.source.split("/")[0]);
        assertEquals("com.tickets.generated", data.type);
        
        String workflowInstanceId = data.source.split("/")[1];
        
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header("ce-id", UUID.randomUUID().toString())
            .header("ce-type", "com.sample.message.messages.waitforack")
            .header("ce-source", "test/1234")
            .header("ce-specversion", "1.0")
            .body("{\n"
                    + "  \"id\" : \"\",\n"
                    + "  \"type\" : \"" + workflowInstanceId + "\",\n"
                    + "  \"owner\" : \"john\"\n"
                    + "}")
        .when()
            .post("/")
        .then()
            .statusCode(204);
        
        events = eventSource.events();
        assertEquals(1, events.size());
        
        data = events.get(0);
        assertEquals("com.sample.message.messages.approved", data.source.split("/")[0]);
        assertEquals("com.tickets.approved", data.type);

    }
    
    // @formatter:on
}