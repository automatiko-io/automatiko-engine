package org.acme.orders;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class ExportImportVerificationTest {
 // @formatter:off
    
    @SuppressWarnings("unchecked")
    @Test
    public void testProcessExecutionWithExportAndImport() {

        String addPayload = "{\"approver\" : \"john\", \"order\": {\"orderNumber\": \"12345\", \"shipped\": false, \"total\": 0}}";
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
        
        String id = "12345";
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
        
        // now let's export that instance and abort it so it can be successfully imported back
        JsonNode exported = given()
            .accept(ContentType.JSON)
        .when()
            .get("/management/processes/orders_1_0/instances/" + id + "/export?abort=true")
        .then()
            .statusCode(200)
            .extract().as(JsonNode.class);  
        
        assertNotNull(exported);
        // after export with abort no instance should be present               
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/api/workflows/v1_0/orders")
        .then().statusCode(200)
            .body("$.size()", is(0)); 
        
        // now, import it back and continue with execution
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(exported.toString())
        .when()
            .post("/management/processes/orders_1_0/instances")
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
            .body("$.size()", is(1));  
        
        taskInfo = given()
                .accept(ContentType.JSON)
            .when()
                .get("/api/workflows/v1_0/orders/" + id + "/tasks?user=john")
            .then()
                .statusCode(200)
                .extract().as(List.class);

        assertEquals(1, taskInfo.size());
        
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
