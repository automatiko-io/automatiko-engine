package org.acme.travels.notification;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.WireMock;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

@QuarkusTest
@TestProfile(WithNotificationsTestProfile.class)
@QuarkusTestResource(org.acme.travels.notification.WiremockCallback.class)
public class VerificationTest {

    @BeforeAll
    public static void configure() {
        configureFor(8089);
    }

    @AfterEach
    public void cleanup() {
        WireMock.resetAllRequests();
    }

    // @formatter:off
    @Test
    public void testProcessTeamsNotification() {

            String addPayload = "{\"name\" : \"john\"}";
            String id = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(addPayload)
                .when()
                    .post("/v1/users?user=mary&group=admin")
                .then()
                    //.log().body(true)
                    .statusCode(200)
                    .body("id", notNullValue(), "name", equalTo("john"), "message", nullValue(), "lastName", nullValue())
                    .extract().path("id");
            
            given()
                .accept(ContentType.JSON)
            .when()
                .get("/v1/users?user=mary&group=admin")
            .then().statusCode(200)
                .body("$.size()", is(1));
            
            given()
                .accept(ContentType.JSON)
            .when()
                .delete("/v1/users/" + id + "?user=mary&group=admin")
            .then().statusCode(200);
            
            verify(1, postRequestedFor(urlEqualTo("/webhook-teams")));
    }
    
    @Test
    public void testProcessSlackNotification() {

            String addPayload = "{\"name\" : \"john\"}";
            String id = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(addPayload)
                .when()
                    .post("/v2/users?user=mary&group=admin")
                .then()
                    //.log().body(true)
                    .statusCode(200)
                    .body("id", notNullValue(), "name", equalTo("john"), "message", nullValue(), "lastName", nullValue())
                    .extract().path("id");
            
            given()
                .accept(ContentType.JSON)
            .when()
                .get("/v2/users?user=mary&group=admin")
            .then().statusCode(200)
                .body("$.size()", is(1));
            
            given()
                .accept(ContentType.JSON)
            .when()
                .delete("/v2/users/" + id + "?user=mary&group=admin")
            .then().statusCode(200);
            
            verify(1, postRequestedFor(urlEqualTo("/webhook-slack")));
    }
    
 // @formatter:on
}
