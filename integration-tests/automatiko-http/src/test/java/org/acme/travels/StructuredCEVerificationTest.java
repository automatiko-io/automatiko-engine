package org.acme.travels;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

@QuarkusTest
@TestProfile(StructuredCloudEventTestProfile.class)
public class StructuredCEVerificationTest {
 // @formatter:off
    
 
    @Test
    public void testProcessSendsAndReceives() throws InterruptedException {
        String id = "bbb";
        String addPayload = "{\n"
                + "  \"person\": {\n"
                + "    \"name\": \"john\",\n"
                + "    \"age\": 41\n"
                + "  }\n"
                + "}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/sends?businessKey=" + id)
            .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue(), "person.name", equalTo("john"), "person.age", equalTo(41));
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/sends")
        .then().statusCode(200)
            .body("$.size()", is(0));
        
        long elapsed = 3000;
        
        while(elapsed >= 0) {
            elapsed -= 1000;
            Thread.sleep(1000);
            int size = given()
                .accept(ContentType.JSON)
            .when()
                .get("/recivers")
            .then().statusCode(200)
                .extract().path("$.size()");
            
            if (size == 1) {
                break;
            }
        }
        
        String receiversId = given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .get("/recivers")
            .then()
                //.log().body(true)
                .statusCode(200)
                .body("[0].id", notNullValue(), "[0].person.name", equalTo("john"), "[0].person.age", equalTo(41))
                .extract().path("[0].id");
        
        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(addPayload)
        .when()
            .delete("/recivers/"+receiversId)
        .then()
            //.log().body(true)
            .statusCode(200);
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/recivers")
        .then().statusCode(200)
            .body("$.size()", is(0));
    }
    
 // @formatter:on
}
