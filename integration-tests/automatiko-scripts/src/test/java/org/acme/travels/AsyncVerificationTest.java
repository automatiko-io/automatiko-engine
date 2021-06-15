package org.acme.travels;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.WireMock;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@QuarkusTestResource(WiremockCallback.class)
public class AsyncVerificationTest {

    @BeforeAll
    public static void configure() {
        configureFor(8088);
    }

    @AfterEach
    public void cleanup() {
        WireMock.resetAllRequests();
    }

 // @formatter:off
    
    @Test
    public void testProcessWithCallback() throws InterruptedException {

        String addPayload = "{\"name\" : \"john\"}";
        given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .header("X-ATK-Mode", "async")
                    .header("X-ATK-Callback", "http://localhost:8088/callback")
                    .body(addPayload)
                    .when()
                        .post("/scripts?businessKey=test")
                    .then()
                        //.log().body(true)
                        .statusCode(202)
                        .body("id", notNullValue(), "name", equalTo("john"), "message", nullValue(), "lastName", nullValue());
        
        long elapsed = 3000;
        
        while(elapsed >= 0) {
            elapsed -= 1000;
            Thread.sleep(1000);
            int size = given()
                .accept(ContentType.JSON)
            .when()
                .get("/scripts")
            .then().statusCode(200)
                .extract().path("$.size()");
            
            if (size == 0) {
                break;
            }
        }
        
        verify(1, postRequestedFor(urlEqualTo("/callback")).withRequestBody(equalToJson("{\"id\":\"test\",\"lastName\": null,\"name\":\"john\",\"message\":\"Hello john\"}")));
    }
    
    @Test
    public void testProcessNoCallback() throws InterruptedException {

        String addPayload = "{\"name\" : \"john\"}";
        given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .header("X-ATK-Mode", "async")
                    .body(addPayload)
                    .when()
                        .post("/scripts?businessKey=test")
                    .then()
                        //.log().body(true)
                        .statusCode(202)
                        .body("id", notNullValue(), "name", equalTo("john"), "message", nullValue(), "lastName", nullValue());
        
        long elapsed = 3000;
        
        while(elapsed >= 0) {
            elapsed -= 1000;
            Thread.sleep(1000);
            int size = given()
                .accept(ContentType.JSON)
            .when()
                .get("/scripts")
            .then().statusCode(200)
                .extract().path("$.size()");
            
            if (size == 0) {
                break;
            }
        }
        
        verify(0, postRequestedFor(urlEqualTo("/callback")));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testProcessWithCallbackWithUserTask() throws InterruptedException {

        String addPayload = "{\"name\" : \"mary\"}";
        given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .header("X-ATK-Mode", "async")
                    .header("X-ATK-Callback", "http://localhost:8088/callback")
                    .body(addPayload)
                    .when()
                        .post("/scripts?businessKey=test")
                    .then()
                        //.log().body(true)
                        .statusCode(202)
                        .body("id", notNullValue(), "name", equalTo("mary"), "message", nullValue(), "lastName", nullValue());
        
        long elapsed = 3000;
        
        while(elapsed >= 0) {
            elapsed -= 1000;
            Thread.sleep(1000);
            int size = given()
                .accept(ContentType.JSON)
            .when()
                .get("/scripts")
            .then().statusCode(200)
                .extract().path("$.size()");
            
            if (size == 1) {
                break;
            }
        }
        
        verify(1, postRequestedFor(urlEqualTo("/callback")).withRequestBody(equalToJson("{\"id\":\"test\",\"lastName\": null,\"name\":\"mary\",\"message\":null}")));
        
        
        List<Map<String, String>> taskInfo = given()
                .accept(ContentType.JSON)
            .when()
                .get("/scripts/test/tasks?user=john")
            .then()
                .statusCode(200)
                .extract().as(List.class);

        assertEquals(1, taskInfo.size());
        
        String taskId = taskInfo.get(0).get("id");
        String taskName = taskInfo.get(0).get("name");
        
        assertEquals("approval", taskName);
        
        String payload = "{}";
        given().
            contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .header("X-ATK-Mode", "async")
            .header("X-ATK-Callback", "http://localhost:8088/callback")
            .body(payload)
        .when()
            .post("/scripts/test/" + taskName + "/" + taskId + "?user=john")
        .then()
            .statusCode(202).body("id", is("test"));

        elapsed = 3000;
        
        while(elapsed >= 0) {
            elapsed -= 1000;
            Thread.sleep(1000);
            int size = given()
                .accept(ContentType.JSON)
            .when()
                .get("/scripts")
            .then().statusCode(200)
                .extract().path("$.size()");
            
            if (size == 0) {
                break;
            }
        }
        
        verify(2, postRequestedFor(urlEqualTo("/callback")));
    }
   
 // @formatter:on
}
