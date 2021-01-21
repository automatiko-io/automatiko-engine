package org.acme.travels;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class VerificationTests {
 // @formatter:off
    
    @Test
    public void testProcessConditionMet() {

        String addPayload = "{\"person\":{\"adult\":true,\"age\":50,\"name\":\"john\"}}";
        given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .body(addPayload)
                    .when()
                        .post("/persons")
                    .then()
                        //.log().body(true)
                        .statusCode(200)
                        .body("id", notNullValue(), "person", notNullValue());
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/persons")
        .then().statusCode(200)
            .body("$.size()", is(0));
    }
    
    @Test
    public void testProcessConditionMetAfterUpdate() {
        String key = "test";
        String addPayload = "{\"person\":{\"adult\":true,\"age\":30,\"name\":\"john\"}}";
        given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .body(addPayload)
                    .when()
                        .post("/persons?businessKey=" + key)
                    .then()
                        //.log().body(true)
                        .statusCode(200)
                        .body("id", notNullValue(), "person", notNullValue());
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/persons")
        .then().statusCode(200)
            .body("$.size()", is(1));
        
        
        List<Map<String, String>> taskInfo = given()
                .accept(ContentType.JSON)
            .when()
                .get("/persons/" + key + "/tasks")
            .then()
                .statusCode(200)
                .extract().as(List.class);

        assertEquals(1, taskInfo.size());
        
        String taskName = taskInfo.get(0).get("name");
        
        assertEquals("Task_Name", taskName);
        
        String updatePayload = "{\"person\":{\"adult\":true,\"age\":50,\"name\":\"john\"}}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(updatePayload)
            .when()
                .post("/persons/" + key)
            .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue(), "person", notNullValue());
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/persons")
        .then().statusCode(200)
            .body("$.size()", is(0));
    }
    
 // @formatter:on
}
