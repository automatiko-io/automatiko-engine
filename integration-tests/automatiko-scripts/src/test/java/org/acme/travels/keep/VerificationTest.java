package org.acme.travels.keep;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

@QuarkusTest
@TestProfile(KeepInstanceTestProfile.class)
public class VerificationTest {
 // @formatter:off
    
    @Test
    public void testProcessNotVersioned() {

        String addPayload = "{\"name\" : \"john\"}";
        given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .body(addPayload)
                    .when()
                        .post("/scripts")
                    .then()
                        //.log().body(true)
                        .statusCode(200)
                        .body("id", notNullValue(), "name", equalTo("john"), "message", equalTo("Hello john"), "lastName", nullValue());
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/scripts")
        .then().statusCode(200)
            .body("$.size()", is(0));
       
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/scripts?status=completed")
        .then().statusCode(200)
            .body("$.size()", is(1));
    }
    
 // @formatter:on
}
