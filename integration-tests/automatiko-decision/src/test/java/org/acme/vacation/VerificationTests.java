package org.acme.vacation;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class VerificationTests {
 // @formatter:off
    
    @Test
    public void testProcessWithDecision() {

        String addPayload = "{\"age\" : 16, \"yearsOfService\" : 1}";
        given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .body(addPayload)
                    .when()
                        .post("/v1_0/vacation")
                    .then()
                        .log().body(true)
                        .statusCode(200)
                        .body("id", notNullValue(), "age", equalTo(16), "yearsOfService", equalTo(1), "vacationDays", equalTo(27));
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/v1_0/vacation")
        .then().statusCode(200)
            .body("$.size()", is(0));
    }
   
 // @formatter:on
}
