package org.automatiko.funq;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class GreetingsTest {
 // @formatter:off
    @Test
    public void testAsJohnEndpoint() {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("{\"name\" : \"john\"}")
        .when()
            .post("/greetings")
        .then()
            .statusCode(200)
            .body("id", notNullValue(), "message", is("Hello john"), "name", is("john"));    
    }
    
    @Test
    public void testAsMaryEndpoint() {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("{\"name\" : \"mary\"}")
        .when()
            .post("/greetings")
        .then()
            .statusCode(200)
            .body("id", notNullValue(), "message", is("Hola mary"), "name", is("mary"));    
    }
    
    @Test
    public void testAsJohnEndpointGet() {
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/greetings?name=john")
        .then()
            .statusCode(200)
            .body("id", notNullValue(), "message", is("Hello john"), "name", is("john"));    
    }
    
    @Test
    public void testAsMaryEndpointGet() {
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/greetings?name=mary")
        .then()
            .statusCode(200)
            .body("id", notNullValue(), "message", is("Hola mary"), "name", is("mary"));    
    }
 // @formatter:on
}