package org.acme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@QuarkusTestResource(WiremockPetstore.class)
public class AsyncVerificationTest {
 // @formatter:off
    
    @Inject
    MockEventPublisher publisher;
    
    @BeforeEach
    public void clear() {
        publisher.clear();
    }
    
    
    @Test
    public void testProcessExecution() throws InterruptedException {

        String addPayload = "{\"userName\" : \"john\", \"petId\" : 1}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/asyncusers")
            .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue());
        
        assertTrue(publisher.waitForNodeCompleted("EndProcess", 3000));
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/asyncusers")
        .then().statusCode(200)
            .body("$.size()", is(0));
        
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProcessExecutionErrorHandled() throws InterruptedException {

        String addPayload = "{\"userName\" : \"jake\", \"petId\" : 1}";
        String key = given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/asyncusers")
            .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue(), "user", nullValue(), "pet", nullValue())
                .extract().path("id");
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/asyncusers")
        .then().statusCode(200)
            .body("$.size()", is(1));
        
        publisher.waitForNodeEntered("User Info", 3000);
        
        
        List<Map<String, String>> taskInfo = given()
                        .accept(ContentType.JSON)
                    .when()
                        .get("/asyncusers/" + key + "/tasks")
                    .then()
                        .statusCode(200)
                        .extract().as(List.class);
        
        assertEquals(1, taskInfo.size());
        
        String taskId = taskInfo.get(0).get("id");
        String taskName = taskInfo.get(0).get("name");
        
        assertEquals("userinfo", taskName);
        
        String payload = "{\"info\" : {\"username\" : \"jake\", \"firstName\" : \"jake\", \"lastName\" : \"doe\", \"email\" : \"jake.doe@email.com\"}}";
        given().
            contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(payload)
        .when()
            .post("/asyncusers/" + key + "/" + taskName + "/" + taskId)
        .then()
            .statusCode(200).body("id", is(key), "user", notNullValue());
        
        publisher.waitForNodeCompleted("User created", 3000);
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/asyncusers")
        .then().statusCode(200)
            .body("$.size()", is(0));
    }
    
    @Test
    public void testProcessExecutionWithUnknownError() throws InterruptedException {

        String addPayload = "{\"userName\" : \"mary\", \"petId\" : 1}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/asyncusers")
            .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue(), "user", nullValue(), "pet", nullValue());
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/asyncusers")
        .then().statusCode(200)
            .body("$.size()", is(1));
        
        long elapsed = 3000;
        
        while(elapsed >= 0) {
            elapsed -= 1000;
            Thread.sleep(1000);
            int size = given()
                .accept(ContentType.JSON)
            .when()
                .get("/asyncusers")
            .then().statusCode(200)
                .extract().path("$.size()");
            
            if (size == 0) {
                break;
            }
        }
        
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testProcessExecutionWithCreateUserError() throws InterruptedException {

        String addPayload = "{\"userName\" : \"mike\", \"petId\" : 1}";
        String key = given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/asyncusers")
            .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue(), "user", nullValue(), "pet", nullValue())
                .extract().path("id");;
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/asyncusers")
        .then().statusCode(200)
            .body("$.size()", is(1));
        
        List<Map<String, String>> taskInfo = given()
                .accept(ContentType.JSON)
            .when()
                .get("/asyncusers/" + key + "/tasks")
            .then()
                .statusCode(200)
                .extract().as(List.class);

        assertEquals(1, taskInfo.size());
        
        String taskId = taskInfo.get(0).get("id");
        String taskName = taskInfo.get(0).get("name");
        
        assertEquals("userinfo", taskName);
        
        String payload = "{\"info\" : {\"username\" : \"mike\", \"firstName\" : \"mike\", \"lastName\" : \"doe\", \"email\" : \"mike.doe@email.com\"}}";
        given().
            contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(payload)
        .when()
            .post("/asyncusers/" + key + "/" + taskName + "/" + taskId)
        .then()
            .statusCode(200).body("id", is(key), "user", notNullValue());
        
        long elapsed = 5000;
        
        while(elapsed >= 0) {
            elapsed -= 1000;
            Thread.sleep(1000);
            int size = given()
                .accept(ContentType.JSON)
            .when()
                .get("/asyncusers")
            .then().statusCode(200)
                .extract().path("$.size()");
            
            if (size == 0) {
                break;
            }
        }
        
        given()
        .accept(ContentType.JSON)
    .when()
        .get("/asyncusers")
    .then().statusCode(200)
        .body("$.size()", is(0));
    }
    
 // @formatter:on
}
