package org.acme.travels;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.workflow.Process;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class VerificationTest {
 // @formatter:off
    
    @Inject
    @Named("scripts")
    Process<?> process;
    
    @Test
    public void testProcessCompleteViaTimer() throws InterruptedException {
        String id = "test";
        String addPayload = "{\"name\" : \"john\"}";
        given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .body(addPayload)
                    .when()
                        .post("/scripts?businessKey=" + id)
                    .then()
                        //.log().body(true)
                        .statusCode(200)
                        .body("id", notNullValue(), "name", equalTo("john"), "message", equalTo("Hello john"), "lastName", nullValue());
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/scripts?sortAsc=true&sortBy=businessKey")
        .then().statusCode(200)
            .body("$.size()", is(1));
        
        long elapsed = 3000;
        
        while(elapsed >= 0) {
            elapsed -= 1000;
            Thread.sleep(1000);
            int size = given()
                .accept(ContentType.JSON)
            .when()
                .get("/scripts")
            .then().statusCode(200)
                .extract().path("$.size()");
            
            if (size == 0) {
                break;
            }
        }
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/scripts")
        .then().statusCode(200)
            .body("$.size()", is(0));
    }
    
    @Test
    public void testProcessAbortOnTimer() {
        String id = "test";
        String addPayload = "{\"name\" : \"john\"}";
        given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .body(addPayload)
                    .when()
                        .post("/scripts?businessKey=" + id)
                    .then()
                        //.log().body(true)
                        .statusCode(200)
                        .body("id", notNullValue(), "name", equalTo("john"), "message", equalTo("Hello john"), "lastName", nullValue());
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/scripts?sortAsc=true&sortBy=startDate")
        .then().statusCode(200)
            .body("$.size()", is(1));
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/scripts?tags="+id)
        .then().statusCode(200)
            .body("$.size()", is(1));
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/scripts?tags=wrong")
        .then().statusCode(200)
            .body("$.size()", is(0));   
        
        Collection<String> ids = process.instances().locateByIdOrTag(id);
        assertEquals(1, ids.size());
        
        given()
            .accept(ContentType.JSON)
        .when()
            .delete("/scripts/" + id)
        .then().statusCode(200);
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/scripts")
        .then().statusCode(200)
            .body("$.size()", is(0));
    }
    
    @Test
    public void testProcessDirectly() {

        String addPayload = "{\"name\" : \"rob\"}";
        given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .body(addPayload)
                    .when()
                        .post("/scripts?metadata=true")
                    .then()
                        //.log().body(true)
                        .statusCode(200)
                        .body("id", notNullValue(), "name", equalTo("rob"), "message", nullValue(), "lastName", nullValue(),
                                "metadata.startDate", notNullValue(), "metadata.endDate", notNullValue(), "metadata.expiredAtDate", notNullValue());
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/scripts")
        .then().statusCode(200)
            .body("$.size()", is(0));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testProcessCompleteViaUserTask() {
        String id = "test";
        String addPayload = "{\"name\" : \"mary\"}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/scripts?businessKey=" + id)
            .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue(), "name", equalTo("mary"), "message", nullValue(), "lastName", nullValue());
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/scripts?sortAsc=true&sortBy=variables.name")
        .then().statusCode(200)
            .body("$.size()", is(1));
        
        List<Map<String, String>> taskInfo = given()
                .accept(ContentType.JSON)
            .when()
                .get("/scripts/" + id + "/tasks?user=john")
            .then()
                .statusCode(200)
                .extract().as(List.class);

        assertEquals(1, taskInfo.size());
        
        String taskId = taskInfo.get(0).get("id");
        String taskName = taskInfo.get(0).get("name");
        
        assertEquals("complete", taskName);
        
        String payload = "{}";
        given().
            contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(payload)
        .when()
            .post("/scripts/" + id + "/" + taskName + "/" + taskId + "?user=john")
        .then()
            .statusCode(200).body("id", is(id), "message", nullValue());
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/scripts")
        .then().statusCode(200)
            .body("$.size()", is(0));
    }
    
 // @formatter:on
}
