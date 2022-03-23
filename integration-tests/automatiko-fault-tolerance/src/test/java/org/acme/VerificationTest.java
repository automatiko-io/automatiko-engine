package org.acme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;

import javax.inject.Inject;

import org.acme.service.FragileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.automatiko.quarkus.tests.FaultToleranceTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;

@TestProfile(value = FaultToleranceTestProfile.class)
@QuarkusTest
public class VerificationTest {
 // @formatter:off
    
    @Inject
    FragileService service;
    
    @Inject
    CircuitBreakerMaintenance maintenance;
    
    @BeforeEach
    public void configure() {
        service.reset();
        maintenance.resetAll();
    }
    
    @Test
    public void testProcessSuccessfullCall() {

        String addPayload = "{}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/fragile")
            .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue(), "result", notNullValue());
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/fragile")
        .then().statusCode(200)
            .body("$.size()", is(0));
    }
    
    @Test
    public void testProcessErrorHandledByBoundary() {

        service.toogle("400");
        
        String addPayload = "{}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/fragile")
            .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue(), "result", nullValue(), "error", notNullValue(), "errorSub", nullValue());
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/fragile")
        .then().statusCode(200)
            .body("$.size()", is(0));
    }
    
    @Test
    public void testProcessErrorHandledByEventSubprocess() {

        service.toogle("409");
        
        String addPayload = "{}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/fragile")
            .then()
                //.log().body(true)
                .statusCode(409)
                .body("error", equalTo("failed with 409"));
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/fragile")
        .then().statusCode(200)
            .body("$.size()", is(0));
    }
    
    @Test
    public void testProcessFailing() throws InterruptedException {
        
        service.toogle("500");

        String addPayload = "{}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/fragile")
            .then()
                //.log().body(true)
                .statusCode(500)
                .body("id", notNullValue(), "message", equalTo("WorkItem execution failed with error code 500"));
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/fragile?status=error")
        .then().statusCode(200)
            .body("$.size()", is(1));
        
        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(addPayload)
        .when()
            .post("/fragile")
        .then()
            //.log().body(true)
            .statusCode(500)
            .body("id", notNullValue(), "message", equalTo("WorkItem execution failed with error code 500"));
        
        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(addPayload)
        .when()
            .post("/fragile")
        .then()
            //.log().body(true)
            .statusCode(500)
            .body("id", notNullValue(), "message", equalTo("WorkItem execution failed with error code 500"));
        
        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(addPayload)
        .when()
            .post("/fragile")
        .then()
            //.log().body(true)
            .statusCode(500)
            .body("id", notNullValue(), "message", equalTo("WorkItem execution failed with error code 500"));
        
        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(addPayload)
        .when()
            .post("/fragile")
        .then()
            //.log().body(true)
            .statusCode(500)
            .body("id", notNullValue(), "message", equalTo("Service not available"));
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/fragile?status=error")
        .then().statusCode(200)
            .body("$.size()", is(5));
        
        service.toogle("500");
        
        Thread.sleep(3000);
        
        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(addPayload)
        .when()
            .post("/fragile")
        .then()
            //.log().body(true)
            .statusCode(200)
            .body("id", notNullValue(), "result", notNullValue());
        

        
        long elapsed = 5000;
        
        while(elapsed >= 0) {
            elapsed -= 1000;
            Thread.sleep(1000);
            int size =  given()
                    .accept(ContentType.JSON)
                    .when()
                        .get("/fragile?status=error")
                    .then().statusCode(200)
                .extract().path("$.size()");
            
            if (size == 0) {
                break;
            }
        }
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/fragile")
        .then().statusCode(200)
            .body("$.size()", is(0));
        
    }
    
    @Test
    public void testProcessTimoutHandling() {
        
        service.toogleBlock();

        String addPayload = "{}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/fragile")
            .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue(), "result", notNullValue(), "error", notNullValue());
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/fragile")
        .then().statusCode(200)
            .body("$.size()", is(0));
    }
 // @formatter:on
}
