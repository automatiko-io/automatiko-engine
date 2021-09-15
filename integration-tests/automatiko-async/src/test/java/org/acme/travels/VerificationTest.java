package org.acme.travels;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.event.DataEvent;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.services.event.ProcessInstanceDataEvent;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class VerificationTest {
 // @formatter:off
    
    @Inject
    MockEventPublisher publisher;
    
    @BeforeEach
    public void clear() {
        publisher.clear();
    }
    
    @Test
    public void testProcessAsyncCallSuccessful() {

        String addPayload = "{\"name\" : \"john\"}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/async")
            .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue(), "name", equalTo("john"));
        
        List<DataEvent<?>> received = publisher.events();
        assertEquals(2, received.size());
        
        ProcessInstanceDataEvent piEvent = (ProcessInstanceDataEvent) received.stream()
                .filter(pi -> ((ProcessInstanceDataEvent) pi).getData().getState().equals(ProcessInstance.STATE_COMPLETED)).findFirst().get();
        assertEquals("Here is async name", piEvent.getData().getVariables().get("name"));
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/async")
        .then().statusCode(200)
            .body("$.size()", is(0));
    }
    
    @Test
    public void testProcessAsyncCallFailure() {

        String addPayload = "{\"name\" : null}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/async")
            .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue(), "name", nullValue());
        
        List<DataEvent<?>> received = publisher.events();
        assertEquals(2, received.size());
        
        ProcessInstanceDataEvent piEvent = (ProcessInstanceDataEvent) received.stream()
                .filter(pi -> ((ProcessInstanceDataEvent) pi).getData().getState().equals(ProcessInstance.STATE_COMPLETED)).findFirst().get();
        assertEquals(null, piEvent.getData().getVariables().get("name"));
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/async")
        .then().statusCode(200)
            .body("$.size()", is(0));
    }
    
    
    @Test
    public void testProcessParentsSuccessful() {

        String addPayload = "{\"name\" : \"john\"}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/parents")
            .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue(), "name", equalTo("john"));
        
        List<DataEvent<?>> received = publisher.events();
        assertEquals(4, received.size());
        
        ProcessInstanceDataEvent piEvent = (ProcessInstanceDataEvent) received.stream()
                .filter(pi -> ((ProcessInstanceDataEvent) pi).getData().getState().equals(ProcessInstance.STATE_COMPLETED)
                        && ((ProcessInstanceDataEvent) pi).getData().getProcessId().equals("parents")).findFirst().get();
        assertEquals("Here is async name", piEvent.getData().getVariables().get("name"));
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/parents")
        .then().statusCode(200)
            .body("$.size()", is(0));
    }
    
    @Test
    public void testProcessParentsFailure() {

        String addPayload = "{\"name\" : null}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/parents")
            .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue(), "name", nullValue());
        
        List<DataEvent<?>> received = publisher.events();
        assertEquals(4, received.size());
        
        ProcessInstanceDataEvent piEvent = (ProcessInstanceDataEvent) received.stream()
                .filter(pi -> ((ProcessInstanceDataEvent) pi).getData().getState().equals(ProcessInstance.STATE_COMPLETED)
                        && ((ProcessInstanceDataEvent) pi).getData().getProcessId().equals("parents")).findFirst().get();
        assertEquals(null, piEvent.getData().getVariables().get("name"));
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/parents")
        .then().statusCode(200)
            .body("$.size()", is(0));
    }
    
    
    @Test
    public void testProcessParallelSuccessful() throws InterruptedException {

        String addPayload = "{}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/parallel")
            .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue());
        
        long elapsed = 3000;
        
        while(elapsed >= 0) {
            elapsed -= 1000;
            Thread.sleep(1000);
            int size = given()
                .accept(ContentType.JSON)
            .when()
                .get("/parallel")
            .then().statusCode(200)
                .extract().path("$.size()");
            
            if (size == 0) {
                break;
            }
        }
        
        List<DataEvent<?>> received = publisher.events();
        assertEquals(5, received.size());
        

        ProcessInstanceDataEvent piEvent = (ProcessInstanceDataEvent) received.stream()
                .filter(pi -> ((ProcessInstanceDataEvent) pi).getData().getState().equals(ProcessInstance.STATE_COMPLETED)).findFirst().get();
        assertEquals("fallback name", piEvent.getData().getVariables().get("errorInfo"));
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/parallel")
        .then().statusCode(200)
            .body("$.size()", is(0));
    }
 // @formatter:on
}
