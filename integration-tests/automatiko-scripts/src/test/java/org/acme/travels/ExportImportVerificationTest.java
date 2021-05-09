package org.acme.travels;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class ExportImportVerificationTest {
 // @formatter:off
    
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
        
        // now let's export that instance and abort it so it can be successfully imported back
        JsonNode exported = given()
            .accept(ContentType.JSON)
        .when()
            .get("/management/processes/scripts/instances/" + id + "/export?abort=true&user=mary")
        .then()
            .statusCode(200)
            .extract().as(JsonNode.class);  
        
        assertNotNull(exported);
        // after export with abort no instance should be present               
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/scripts?user=mary")
        .then().statusCode(200)
            .body("$.size()", is(0));
        
        // now, import it back and continue with execution
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(exported.toString())
        .when()
            .post("/management/processes/scripts/instances?user=mary")
        .then()
                //.log().body(true)
            .statusCode(200)
            .body("id", notNullValue());
        
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
 // @formatter:on
}
