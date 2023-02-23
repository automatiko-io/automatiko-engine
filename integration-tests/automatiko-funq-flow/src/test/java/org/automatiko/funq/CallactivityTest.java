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
public class CallactivityTest {

    @Inject
    MockEventSource eventSource;

    // @formatter:off
    @Test
    public void testStartFunctionEndpoint() {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header("ce-id", UUID.randomUUID().toString())
            .header("ce-type", "com.sample.parent.parent")
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
        assertEquals("com.sample.parent.parent", data.source.split("/")[0]);
        assertEquals("com.sample.parent.parent.callanother", data.type);
        
        String workflowInstanceId = data.source.split("/")[1];        
        
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header("ce-id", UUID.randomUUID().toString())
            .header("ce-type", "com.sample.parent.parent.callanother")
            .header("ce-source", "test")
            .header("ce-specversion", "1.0")
            .body("{\n"
                    + "  \"id\" : \"" + workflowInstanceId + "\",\n"
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
        
        events = eventSource.events();
        assertEquals(1, events.size());
        
        data = events.get(0);
        assertEquals("com.sample.message.messages.SendTicket", data.source.split("/")[0]);
        assertEquals("com.tickets.generated", data.type);
        
        String subWorkflowInstanceId = data.source.split("/")[1];
        
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header("ce-id", UUID.randomUUID().toString())
            .header("ce-type", "com.sample.message.messages.waitforack")
            .header("ce-source", "test/1234")
            .header("ce-subject", subWorkflowInstanceId)
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
        assertEquals("com.sample.message.messages.receiveticket_0", data.source.split("/")[0]);
        assertEquals("com.sample.message.messages.logafterreceive", data.type);
        
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header("ce-id", UUID.randomUUID().toString())
            .header("ce-type", "com.sample.message.messages.logafterreceive")
            .header("ce-source", "test")
            .header("ce-specversion", "1.0")
            .body("{\n"
                    + "  \"id\" : \"" + subWorkflowInstanceId + "\",\n"
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
        
        events = eventSource.events();
        assertEquals(2, events.size());
        
        data = events.get(0);
        assertEquals("com.sample.message.messages.approved", data.source.split("/")[0]);
        assertEquals("com.tickets.approved", data.type);
        
        data = events.get(1);
        assertEquals("com.sample.message.messages.logafterreceive", data.source.split("/")[0]);
        assertEquals("com.sample.parent.parent.done", data.type);
        
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header("ce-id", UUID.randomUUID().toString())
            .header("ce-type", "com.sample.parent.parent.logafter")
            .header("ce-source", "test")
            .header("ce-specversion", "1.0")
            .body("{\n"
                    + "  \"id\" : \"" + workflowInstanceId + "\",\n"
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
        
        events = eventSource.events();
        assertEquals(1, events.size());
        
        data = events.get(0);
        assertEquals("com.sample.parent.parent.logafter", data.source.split("/")[0]);
        assertEquals("com.sample.parent.parent.done", data.type);

    }
    
    // @formatter:on
}