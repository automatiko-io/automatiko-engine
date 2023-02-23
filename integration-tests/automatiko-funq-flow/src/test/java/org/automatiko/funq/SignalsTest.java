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
public class SignalsTest {

    @Inject
    MockEventSource eventSource;

    // @formatter:off
    @Test
    public void testStartFunctionEndpoint() {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header("ce-id", UUID.randomUUID().toString())
            .header("ce-type", "com.sample.signals.sendAndReceive")
            .header("ce-source", "test")
            .header("ce-specversion", "1.0")
            .body("{\n"
                    + "  \"approved\" : true,\n"
                    + "  \"key\" : \"test\",\n"
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
        assertEquals(0, events.size());
        

        String workflowInstanceId = "test";
        
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header("ce-id", UUID.randomUUID().toString())
            .header("ce-type", "com.sample.signals.sendAndReceive.waitforack")
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
        
        EventData data = events.get(0);
        assertEquals("com.sample.signals.sendAndReceive.acknowledge_0", data.source.split("/")[0]);
        assertEquals("com.sample.signals.sendAndReceive.completed", data.type);

    }
    
    // @formatter:on
}