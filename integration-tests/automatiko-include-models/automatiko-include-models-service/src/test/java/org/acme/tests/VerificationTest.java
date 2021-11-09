package org.acme.tests;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class VerificationTest {
 // @formatter:off
    
    @Test
    public void testProcessInvocationWithSharedModule() {

        String addPayload = "{\n"
                + "  \"address\": {\n"
                + "    \"street\": \"main\",\n"
                + "    \"city\": \"New York\",\n"
                + "    \"country\": \"USA\",\n"
                + "    \"zipCode\": \"10000\",\n"
                + "    \"code\": \"+1\"\n"
                + "  }\n"
                + "}";
        given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .body(addPayload)
                    .when()
                        .post("/registration")
                    .then()
                        //.log().body(true)
                        .statusCode(200)
                        .body("id", notNullValue(), "address", notNullValue(), "address.code", equalTo("000"));
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/registration")
        .then().statusCode(200)
            .body("$.size()", is(0));
    }
    
 // @formatter:on
}
