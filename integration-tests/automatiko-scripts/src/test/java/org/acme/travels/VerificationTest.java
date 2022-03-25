package org.acme.travels;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
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
    
    @Test
    public void testProcessNotVersionedWithMetadata() {

        String addPayload = "{\"name\" : \"john\"}";
        given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .body(addPayload)
                    .when()
                        .post("/scripts?metadata=true")
                    .then()
                        //.log().body(true)
                        .statusCode(200)
                        .body("id", notNullValue(), "name", equalTo("john"), "message", equalTo("Hello john"), "lastName", nullValue(),
                                "metadata.description", equalTo("Simple script handling workflow for john"),
                                "metadata.state", equalTo(2),
                                "metadata.id", notNullValue(),
                                "metadata.businessKey", nullValue(),
                                "metadata.tags.size()", is(1),
                                "metadata.tags[0]", equalTo("john"));
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/scripts")
        .then().statusCode(200)
            .body("$.size()", is(0));
    }
    @Test
    public void testProcessNotVersionedWithInitiatorWithMetadata() {

        String addPayload = "{\"name\" : \"mary\"}";
        String id = given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/scripts?user=mary&metadata=true")
            .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue(), "name", equalTo("mary"), "message", nullValue(), "lastName", nullValue(),
                        "metadata.description", equalTo("Simple script handling workflow for mary"),
                        "metadata.state", equalTo(1),
                        "metadata.id", notNullValue(),
                        "metadata.businessKey", nullValue(),
                        "metadata.tags.size()", is(1),
                        "metadata.tags[0]", equalTo("mary"))
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
            .get("/scripts?user=mary&metadata=true")
        .then().statusCode(200)
            .body("$.size()", is(1),
                    "[0].metadata.description", equalTo("Simple script handling workflow for mary"),
                    "[0].metadata.state", equalTo(1),
                    "[0].metadata.id", notNullValue(),
                    "[0].metadata.businessKey", nullValue(),
                    "[0].metadata.tags.size()", is(1),
                    "[0].metadata.tags[0]", equalTo("mary"));
 
        given()
            .accept(ContentType.JSON)
        .when()
            .delete("/scripts/" + id)
        .then().statusCode(403);        
        
        given()
            .accept(ContentType.JSON)
        .when()
            .delete("/scripts/" + id + "?user=mary&metadata=true")
        .then().statusCode(200)
            .body("id", notNullValue(), "name", equalTo("mary"), "message", nullValue(), "lastName", nullValue(),
                "metadata.description", equalTo("Simple script handling workflow for mary"),
                "metadata.state", equalTo(3),
                "metadata.id", notNullValue(),
                "metadata.businessKey", nullValue(),
                "metadata.tags.size()", is(1),
                "metadata.tags[0]", equalTo("mary"));
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/scripts?user=mary")
        .then().statusCode(200)
            .body("$.size()", is(0));        
    }
    
    @Test
    public void testProcessWithErrorEndEvent() {

        String addPayload = "{\"name\" : \"john\"}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/errors")
            .then()
                //.log().body(true)
                .statusCode(410)
                .body("details", equalTo("here is the error"));
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/errors")
        .then().statusCode(200)
            .body("$.size()", is(0));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testProcessWithErrorEndEventAfterWaitState() {

        String addPayload = "{\"name\" : \"mary\"}";
        String id = given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/errors")
            .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue(), "name", equalTo("mary"), "message", nullValue(), "lastName", nullValue())
                .extract().path("id");
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/errors")
        .then().statusCode(200)
            .body("$.size()", is(1));
        
        
        List<Map<String, String>> taskInfo = given()
                .accept(ContentType.JSON)
            .when()
                .get("/errors/" + id + "/tasks?user=john")
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
                .get("/errors/" + id + "/approval/" + taskId + "?user=john")
            .then()
                .statusCode(200)
                .extract().as(Map.class);
        
        assertEquals("mary", taskData.get("name"));        
        
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("{}")
            .when()
                .post("/errors/" + id + "/approval/" + taskId + "?user=john")
            .then()
                //.log().body(true)
                .statusCode(427)
                .body("details", equalTo("another error"));
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/errors")
        .then().statusCode(200)
            .body("$.size()", is(0));  
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testProcessUserInfoMetadataLinks() {

        String addPayload = "{\"name\" : \"john\"}";
        String id = given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/users?user=mary&group=admin&metadata=true&businessKey=test")
            .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue(), "name", equalTo("john"), "message", nullValue(), "lastName", nullValue(),
                        "metadata.links[0].url", startsWith("/users/098f6bcd-4621-3373-8ade-4e832627b4f6/approval/"),
                        "metadata.links[0].form", startsWith("/management/tasks/link/"))
                .extract().path("id");
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/users?user=mary&group=admin&metadata=true")
        .then().statusCode(200)
            //.log().body(true)
            .body("$.size()", is(1),
                    "[0].metadata.links[0].url", startsWith("/users/098f6bcd-4621-3373-8ade-4e832627b4f6/approval/"),
                    "[0].metadata.links[0].form", startsWith("/management/tasks/link/"));
        
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
