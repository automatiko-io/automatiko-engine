package org.acme.travels;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class VerificationTest {
 // @formatter:off
    
    @Test
    public void testProcessNotVersioned() {

        String addPayload = "{\"name\" : \"john\"}";
        given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .body(addPayload)
                    .when()
                        .post("/scripts")
                    .then()
                        //.log().body(true)
                        .statusCode(200)
                        .body("id", notNullValue(), "name", equalTo("john"), "message", equalTo("Hello john"), "lastName", nullValue());
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/scripts")
        .then().statusCode(200)
            .body("$.size()", is(0));
    }
    
    @Test
    public void testProcessVersionOne() {

        String addPayload = "{\"name\" : \"john\"}";
        given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .body(addPayload)
                    .when()
                        .post("/v1/scripts")
                    .then()
                        //.log().body(true)
                        .statusCode(200)
                        .body("id", notNullValue(), "name", equalTo("john"), "message", equalTo("Hello john"));
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/v1/scripts")
        .then().statusCode(200)
            .body("$.size()", is(0));
    }
    
    @Test
    public void testProcessVersionTwo() {

        String addPayload = "{\"firstname\" : \"john\"}";
        given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .body(addPayload)
                    .when()
                        .post("/v2/scripts")
                    .then()
                        //.log().body(true)
                        .statusCode(200)
                        .body("id", notNullValue(), "firstname", equalTo("john"), "message", equalTo("Hello john"));
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/v2/scripts")
        .then().statusCode(200)
            .body("$.size()", is(0));
    }
    
    @Test
    public void testProcessNotVersionedWithInitiator() {

        String addPayload = "{\"name\" : \"mary\"}";
        String id = given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/scripts?user=mary")
            .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue(), "name", equalTo("mary"), "message", nullValue(), "lastName", nullValue())
                .extract().path("id");
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/scripts")
        .then().statusCode(200)
            .body("$.size()", is(0));
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/scripts?user=mary")
        .then().statusCode(200)
            .body("$.size()", is(1));
 
        given()
            .accept(ContentType.JSON)
        .when()
            .delete("/scripts/" + id)
        .then().statusCode(403);        
        
        given()
            .accept(ContentType.JSON)
        .when()
            .delete("/scripts/" + id + "?user=mary")
        .then().statusCode(200);
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/scripts?user=mary")
        .then().statusCode(200)
            .body("$.size()", is(0));        
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testProcessUserInfo() {

        String addPayload = "{\"name\" : \"john\"}";
        String id = given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/users?user=mary&group=admin")
            .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue(), "name", equalTo("john"), "message", nullValue(), "lastName", nullValue())
                .extract().path("id");
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/users?user=mary&group=admin")
        .then().statusCode(200)
            .body("$.size()", is(1));
        
        List<Map<String, String>> taskInfo = given()
                .accept(ContentType.JSON)
            .when()
                .get("/users/" + id + "/tasks?user=mary&group=admin")
            .then()
                .statusCode(200)
                .extract().as(List.class);

        assertEquals(1, taskInfo.size());
        
        String taskId = taskInfo.get(0).get("id");
        String taskName = taskInfo.get(0).get("name");
        
        assertEquals("approval", taskName);
        
        Map<String, String> taskData = given()
                .accept(ContentType.JSON)
            .when()
                .get("/users/" + id + "/approval/" + taskId + "?user=mary&group=admin")
            .then()
                .statusCode(200)
                .extract().as(Map.class);
        
        assertEquals("mary", taskData.get("name"));
        assertEquals(true, taskData.get("admin"));
        
        given()
            .accept(ContentType.JSON)
        .when()
            .delete("/users/" + id + "?user=mary&group=admin")
        .then().statusCode(200);        
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/users?user=mary&group=admin")
        .then().statusCode(200)
            .body("$.size()", is(0));  
    }
 // @formatter:on
}
