package org.acme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import org.acme.service.FragileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import jakarta.inject.Inject;

@TestProfile(value = UOWFaultToleranceTestProfile.class)
@QuarkusTest
public class VerificationUOWTimeoutTest {
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
                .statusCode(500)
                .body("message", equalTo("Operation timed out"));
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/fragile")
        .then().statusCode(200)
            .body("$.size()", is(0));
    }  
 // @formatter:on
}
