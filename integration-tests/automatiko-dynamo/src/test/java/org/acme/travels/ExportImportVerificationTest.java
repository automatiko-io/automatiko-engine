package org.acme.travels;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@QuarkusTestResource(DynamoDBContainer.class)
public class ExportImportVerificationTest {
 // @formatter:off
    
    
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
            .get("/scripts")
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
        
        // now let's export that instance and abort it so it can be successfully imported back
        JsonNode exported = given()
            .accept(ContentType.JSON)
        .when()
            .get("/management/processes/scripts/instances/" + id + "/export?abort=true")
        .then()
            .statusCode(200)
            .extract().as(JsonNode.class);  
        
        assertNotNull(exported);
        // after export with abort no instance should be present               
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/scripts")
        .then().statusCode(200)
            .body("$.size()", is(0));
        
        // now, import it back and continue with execution
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(exported.toString())
        .when()
            .post("/management/processes/scripts/instances")
        .then()
                //.log().body(true)
            .statusCode(200)
            .body("id", notNullValue());
        
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
