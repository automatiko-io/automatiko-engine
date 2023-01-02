package org.acme.travels;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@QuarkusTest
public class VerificationTest {

    @Inject
    RestClient elasticClient;

 // @formatter:off
    
    @SuppressWarnings("unchecked")
    @Test
    public void testProcessExecution() throws Exception {
        String key = "john";
        String addPayload = "{\"name\" : \"john\"}";
        
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/v1/omboarding?businessKey=" + key)
            .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", equalTo("john"), "person", nullValue());
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/v1/omboarding?sortAsc=true&sortBy=businessKey")
        .then().statusCode(200)
            .body("$.size()", is(1));
        
        Request request = new Request(
                "GET",
                "/omboarding_1/_search");
        // since elastic calls are done async from automatiko publisher give it a bit of time to be completed
        Thread.sleep(1000);
        Response response = elasticClient.performRequest(request);
        String responseBody = EntityUtils.toString(response.getEntity());
        JsonObject json = new JsonObject(responseBody);
        JsonArray hits = json.getJsonObject("hits").getJsonArray("hits");
    
        assertEquals(1, hits.size());
        JsonObject item = hits.getJsonObject(0).getJsonObject("_source");
        assertEquals("john", item.getString("name"));
        
        // check if tasks are loaded to elastic
        request = new Request(
                "GET",
                "/tasks/_search");
        response = elasticClient.performRequest(request);
        responseBody = EntityUtils.toString(response.getEntity());
        json = new JsonObject(responseBody);
        hits = json.getJsonObject("hits").getJsonArray("hits");
    
        assertEquals(1, hits.size());
        item = hits.getJsonObject(0).getJsonObject("_source");
        assertEquals("Fill in person details", item.getString("name"));
        assertEquals("personInfo", item.getJsonObject("_metadata").getString("referenceName"));
        
        List<Map<String, String>> taskInfo = given()
                        .accept(ContentType.JSON)
                    .when()
                        .get("/v1/omboarding/" + key + "/tasks")
                    .then()
                        .statusCode(200)
                        .extract().as(List.class);
        
        assertEquals(1, taskInfo.size());
        
        String taskId = taskInfo.get(0).get("id");
        String taskName = taskInfo.get(0).get("name");
        
        assertEquals("personInfo", taskName);
        
        String payload = "{\"person\" : {\"name\" : \"john\", \"lastName\" : \"doe\", \"age\" : 40}}";
        given().
            contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(payload)
        .when()
            .post("/v1/omboarding/" + key + "/" + taskName + "/" + taskId)
        .then()
            .statusCode(200).body("id", is(key), "person", notNullValue());
        
        
        taskInfo = given()
                .accept(ContentType.JSON)
            .when()
                .get("/v1/omboarding/" + key + "/tasks")
            .then()
                .statusCode(200)
                .extract().as(List.class);

        assertEquals(1, taskInfo.size());
        
        taskId = taskInfo.get(0).get("id");
        taskName = taskInfo.get(0).get("name");
        
        assertEquals("approve", taskName);
        
        payload = "{\"name\" : \"john\", \"lastName\" : \"doe\", \"age\" : 45}";
        given().
            contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(payload)
        .when()
            .post("/v1/omboarding/" + key + "/" + taskName + "/" + taskId)
        .then()
            .statusCode(200).body("id", is(key), "person", notNullValue());
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/v1/omboarding")
        .then().statusCode(200)
            .body("$.size()", is(0));
        
        
        // next instance for the same name should complete directly returning valid person
        Map<String, Object> person = given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(addPayload)
        .when()
            .post("/v1/omboarding?businessKey=" + key)
        .then()
            //.log().body(true)
            .statusCode(200)
            .body("id", equalTo("john"), "person", notNullValue()).extract().path("person");
        
        assertEquals("john", person.get("name"));
        assertEquals("doe", person.get("lastName"));
        assertEquals(45, person.get("age"));
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/v1/omboarding")
        .then().statusCode(200)
            .body("$.size()", is(0));        
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testProcessExecutionV2() {
        String key = "mary";
        String addPayload = "{\"name\" : \"mary\"}";
        
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/v2/omboarding?businessKey=" + key)
            .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", equalTo("mary"), "person", nullValue());
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/v2/omboarding?sortAsc=true&sortBy=startDate")
        .then().statusCode(200)
            .body("$.size()", is(1));
        
        List<Map<String, String>> taskInfo = given()
                        .accept(ContentType.JSON)
                    .when()
                        .get("/v2/omboarding/" + key + "/tasks")
                    .then()
                        .statusCode(200)
                        .extract().as(List.class);
        
        assertEquals(1, taskInfo.size());
        
        String taskId = taskInfo.get(0).get("id");
        String taskName = taskInfo.get(0).get("name");
        
        assertEquals("personInfo", taskName);
        
        String payload = "{\"person\" : {\"name\" : \"mary\", \"lastName\" : \"doe\", \"age\" : 40}}";
        given().
            contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(payload)
        .when()
            .post("/v2/omboarding/" + key + "/" + taskName + "/" + taskId)
        .then()
            .statusCode(200).body("id", is(key), "person", notNullValue());
        
        
        taskInfo = given()
                .accept(ContentType.JSON)
            .when()
                .get("/v2/omboarding/" + key + "/tasks")
            .then()
                .statusCode(200)
                .extract().as(List.class);

        assertEquals(1, taskInfo.size());
        
        taskId = taskInfo.get(0).get("id");
        taskName = taskInfo.get(0).get("name");
        
        assertEquals("approve", taskName);
        
        payload = "{\"name\" : \"mary\", \"lastName\" : \"doe\", \"age\" : 45}";
        given().
            contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(payload)
        .when()
            .post("/v2/omboarding/" + key + "/" + taskName + "/" + taskId)
        .then()
            .statusCode(200).body("id", is(key), "person", notNullValue());
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/v2/omboarding")
        .then().statusCode(200)
            .body("$.size()", is(0));
        
        
        // next instance for the same name should complete directly returning valid person
        Map<String, Object> person = given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(addPayload)
        .when()
            .post("/v2/omboarding?businessKey=" + key)
        .then()
            //.log().body(true)
            .statusCode(200)
            .body("id", equalTo("mary"), "person", notNullValue()).extract().path("person");
        
        assertEquals("mary", person.get("name"));
        assertEquals("doe", person.get("lastName"));
        assertEquals(45, person.get("age"));
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/v2/omboarding")
        .then().statusCode(200)
            .body("$.size()", is(0));        
    }
    
    @Test
    public void testProcessTimeTrackExecution() throws InterruptedException {

        String addPayload = "{}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/timetrack")
            .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue());
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/timetrack")
        .then().statusCode(200)
            .body("$.size()", is(1));
        
        long elapsed = 3000;
        
        while(elapsed >= 0) {
            elapsed -= 1000;
            Thread.sleep(1000);
            int size = given()
                .accept(ContentType.JSON)
            .when()
                .get("/timetrack")
            .then().statusCode(200)
                .extract().path("$.size()");
            
            if (size == 0) {
                break;
            }
        }
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/timetrack")
        .then().statusCode(200)
            .body("$.size()", is(0));
    }
    
 // @formatter:on
}
