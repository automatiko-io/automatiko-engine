package org.acme.travels;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class VerificationTest {
 // @formatter:off
    
    @Test
    public void testProcessExecution() throws InterruptedException {

        String addPayload = "{}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/timers")
            .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue());
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/timers")
        .then().statusCode(200)
            .body("$.size()", is(1));
        
        long elapsed = 3000;
        
        while(elapsed >= 0) {
            elapsed -= 1000;
            Thread.sleep(1000);
            int size = given()
                .accept(ContentType.JSON)
            .when()
                .get("/timers")
            .then().statusCode(200)
                .extract().path("$.size()");
            
            if (size == 0) {
                break;
            }
        }
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/timers")
        .then().statusCode(200)
            .body("$.size()", is(0));
    }
    
    @Test
    public void testProcessExecutionCronTimer() throws InterruptedException {

        String addPayload = "{}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/timerscron")
            .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue());
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/timerscron")
        .then().statusCode(200)
            .body("$.size()", is(1));
        
        long elapsed = 3000;
        
        while(elapsed >= 0) {
            elapsed -= 1000;
            Thread.sleep(1000);
            int size = given()
                .accept(ContentType.JSON)
            .when()
                .get("/timerscron")
            .then().statusCode(200)
                .extract().path("$.size()");
            
            if (size == 0) {
                break;
            }
        }
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/timerscron")
        .then().statusCode(200)
            .body("$.size()", is(0));
    }
    
 // @formatter:on
}
