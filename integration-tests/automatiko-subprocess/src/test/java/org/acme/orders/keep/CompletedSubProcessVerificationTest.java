package org.acme.orders.keep;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

@QuarkusTest
@TestProfile(KeepInstanceTestProfile.class)
public class CompletedSubProcessVerificationTest {
 // @formatter:off
    
    @AfterEach
    public void clear() {
        System.clearProperty("terminate");
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testProcessExecution() {    
        System.setProperty("terminate", "false");
        String addPayload = "{\"approver\" : \"john\", \"order\": {\"orderNumber\": \"99999\", \"shipped\": false, \"total\": 0}}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/api/workflows/v1_0/orders")
            .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue(), "approver", equalTo("john"), "order", notNullValue());
        
        String id = "99999";
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/api/workflows/v1_0/orders")
        .then().statusCode(200)
            .body("$.size()", is(1));
        
        // there should be one instance of order items        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/api/workflows/v1_0/orders/" + id + "/orderItems")
        .then()
            .statusCode(200)
            .body("$.size()", is(1));  
        
        // and one task coming from it
        List<Map<String, String>> taskInfo = given()
                .accept(ContentType.JSON)
            .when()
                .get("/api/workflows/v1_0/orders/" + id + "/tasks?user=john")
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
            .post("/api/workflows/v1_0/orders/" + id + "/orderItems/john/" + taskName + "/" + taskId + "?user=john")
        .then()
            .statusCode(200).body("id", is("john"));
        
        // there should be one instance of order item acceptance      
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/api/workflows/v1_0/orders/" + id + "/orderItems/john/acceptance")
        .then()
            .statusCode(200)
            .body("$.size()", is(1))
            .extract().path("[0].id");  
        
        taskInfo = given()
                .accept(ContentType.JSON)
            .when()
                .get("/api/workflows/v1_0/orders/" + id + "/tasks?user=john")
            .then()
                .statusCode(200)
                .extract().as(List.class);

        assertEquals(1, taskInfo.size());
        
        taskId = taskInfo.get(0).get("id");
        taskName = taskInfo.get(0).get("name");
        
        given().
            contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(payload)
        .when()
            .post(taskInfo.get(0).get("reference") + "?user=john")
        .then()
            .statusCode(200);
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/api/workflows/v1_0/orders")
        .then().statusCode(200)
            .log().body()
            .body("$.size()", is(1));  
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/management/processes/orders_1_0/instances/99999")
        .then().statusCode(200)
            .body("subprocesses[0].state", equalTo(1), "subprocesses[0].processId", equalTo("orderItems_1"), "id", notNullValue());  
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/management/processes/orderItems_1/instances/99999:john")
        .then().statusCode(200)
            .body("subprocesses[0].state", equalTo(2), "subprocesses[0].processId", equalTo("acceptance_1"), "id", notNullValue());
        
        given()
            .accept(ContentType.JSON)
        .when()
            .delete("/api/workflows/v1_0/orders/" + id)
        .then()
            .statusCode(200);
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/api/workflows/v1_0/orders")
        .then().statusCode(200)
            .body("$.size()", is(0));        
    }
    
 // @formatter:on
}
