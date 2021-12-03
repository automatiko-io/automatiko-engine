package org.acme.travels;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class SubprocessVerificationTest {
 // @formatter:off
    
    @SuppressWarnings("unchecked")
    @Test
    public void testProcessExecution() {

        String addPayload = "{\"approver\" : \"john\", \"order\": {\"orderNumber\": \"12345\", \"shipped\": false, \"total\": 0}}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/v1_0/orders")
            .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue(), "approver", equalTo("john"), "order", notNullValue());
        
        String id = "12345";
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/v1_0/orders")
        .then().statusCode(200)
            .body("$.size()", is(1));
        
        // there should be one instance of order items        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/v1_0/orders/" + id + "/orderItems")
        .then()
            .statusCode(200)
            .body("$.size()", is(1));  
        
        // and one task coming from it
        List<Map<String, String>> taskInfo = given()
                .accept(ContentType.JSON)
            .when()
                .get("/v1_0/orders/" + id + "/tasks?user=john")
            .then()
                .statusCode(200)
                .extract().as(List.class);

        assertEquals(1, taskInfo.size());
        
        String taskId = taskInfo.get(0).get("id");
        String taskName = taskInfo.get(0).get("name");
        
        assertEquals("Verify_order", taskName);
        
        String payload = "{}";
        given().
            contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(payload)
        .when()
            .post("/v1_0/orders/" + id + "/orderItems/john/" + taskName + "/" + taskId + "?user=john")
        .then()
            .statusCode(200).body("id", is("john"));
        
        // there should be one instance of order item acceptance      
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/v1_0/orders/" + id + "/orderItems/john/acceptance")
        .then()
            .statusCode(200)
            .body("$.size()", is(1));  
        
        taskInfo = given()
                .accept(ContentType.JSON)
            .when()
                .get("/v1_0/orders/" + id + "/tasks?user=john")
            .then()
                .statusCode(200)
                .extract().as(List.class);

        assertEquals(1, taskInfo.size());
        
        given()
            .accept(ContentType.JSON)
        .when()
            .delete("/v1_0/orders/" + id)
        .then()
            .statusCode(200);
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/v1_0/orders")
        .then().statusCode(200)
            .body("$.size()", is(0));        
    }
    
 // @formatter:on
}
