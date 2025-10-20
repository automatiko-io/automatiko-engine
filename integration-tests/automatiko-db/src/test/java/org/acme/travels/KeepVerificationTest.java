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
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

@QuarkusTest
@TestProfile(KeepInstanceTestProfile.class)
public class KeepVerificationTest {

 // @formatter:off
    
    @SuppressWarnings("unchecked")
    @Test
    public void testProcessExecution() throws Exception {
        String key = "john";
        String addPayload = "{\"name\" : \"john\"}";
        
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/v1/omboarding?businessKey=" + key)
            .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", equalTo("john"), "person", nullValue());
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/v1/omboarding?sortAsc=true&sortBy=businessKey")
        .then().statusCode(200)
            .body("$.size()", is(1));
     
        // check if tasks are indexed in db 
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/index/usertasks")
        .then().statusCode(200)
            .body("$.size()", is(1));
        
        // check index by using custom query
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/index/usertasks/queries/byProcess?process=omboarding")
        .then().statusCode(200)
            .body("$.size()", is(1));
        
        List<Map<String, String>> taskInfo = given()
                        .accept(ContentType.JSON)
                    .when()
                        .get("/v1/omboarding/" + key + "/tasks")
                    .then()
                        .statusCode(200)
                        .extract().as(List.class);
        
        assertEquals(1, taskInfo.size());
        
        String taskId = taskInfo.get(0).get("id");
        String taskName = taskInfo.get(0).get("name");
        
        assertEquals("personInfo", taskName);
        
        String payload = "{\"person\" : {\"name\" : \"john\", \"lastName\" : \"doe\", \"age\" : 40}}";
        given().
            contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(payload)
        .when()
            .post("/v1/omboarding/" + key + "/" + taskName + "/" + taskId)
        .then()
            .statusCode(200).body("id", is(key), "person", notNullValue());
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/index/usertasks?state=Ready")
        .then().statusCode(200)
            .body("$.size()", is(1));
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/index/usertasks?state=Completed")
        .then().statusCode(200)
            .body("$.size()", is(1));
        
        taskInfo = given()
                .accept(ContentType.JSON)
            .when()
                .get("/v1/omboarding/" + key + "/tasks")
            .then()
                .statusCode(200)
                .extract().as(List.class);

        assertEquals(1, taskInfo.size());
        
        taskId = taskInfo.get(0).get("id");
        taskName = taskInfo.get(0).get("name");
        
        assertEquals("approve", taskName);
        
        payload = "{\"name\" : \"john\", \"lastName\" : \"doe\", \"age\" : 45}";
        given().
            contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(payload)
        .when()
            .post("/v1/omboarding/" + key + "/" + taskName + "/" + taskId)
        .then()
            .statusCode(200).body("id", is(key), "person", notNullValue());
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/v1/omboarding")
        .then().statusCode(200)
            .body("$.size()", is(0));     
       
    }
   
    
 // @formatter:on
}
