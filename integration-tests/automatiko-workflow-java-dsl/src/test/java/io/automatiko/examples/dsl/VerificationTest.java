package io.automatiko.examples.dsl;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@QuarkusTestResource(WiremockPetstore.class)
public class VerificationTest {

    @Test
    public void testHelloWorldWorkflow() {

        String addPayload = "{\"name\" : \"john\"}";
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(addPayload)
                .when()
                .post("/v1/hello")
                .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue(), "name", equalTo("john"));

        given()
                .accept(ContentType.JSON)
                .when()
                .get("/v1/hello")
                .then().statusCode(200)
                .body("$.size()", is(0));

        given()
                .accept(ContentType.JSON)
                .when()
                .get("/v1/hello?status=completed")
                .then().statusCode(200)
                .body("$.size()", is(1));
    }

    @Test
    public void testSplitWorkflow() {

        String addPayload = "{\"x\" : \"test\"}";
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(addPayload)
                .when()
                .post("/split")
                .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue());

        given()
                .accept(ContentType.JSON)
                .when()
                .get("/split")
                .then().statusCode(200)
                .body("$.size()", is(0));
    }

    @Test
    public void testSplitAndJoinWorkflow() {

        String addPayload = "{\"x\" : \"test\"}";
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(addPayload)
                .when()
                .post("/split-and-join")
                .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue());

        given()
                .accept(ContentType.JSON)
                .when()
                .get("/split-and-join")
                .then().statusCode(200)
                .body("$.size()", is(0));
    }

    @Test
    public void testLocalServiceWorkflow() {

        String addPayload = "{\"name\" : \"john\"}";
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(addPayload)
                .when()
                .post("/v1/service")
                .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue(), "greeting", equalTo("Hello john"));

        given()
                .accept(ContentType.JSON)
                .when()
                .get("/v1/service")
                .then().statusCode(200)
                .body("$.size()", is(0));
    }

    @Test
    public void testRestServiceWorkflow() {

        String addPayload = "{\"petId\" : 1}";
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(addPayload)
                .when()
                .post("/v1/rest-service")
                .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue(), "pet", notNullValue());

        given()
                .accept(ContentType.JSON)
                .when()
                .get("/v1/rest-service")
                .then().statusCode(200)
                .body("$.size()", is(0));

        addPayload = "{\"petId\" : 3000}";
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(addPayload)
                .when()
                .post("/v1/rest-service")
                .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue(), "pet", nullValue());

        given()
                .accept(ContentType.JSON)
                .when()
                .get("/v1/rest-service")
                .then().statusCode(200)
                .body("$.size()", is(0));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUserTaskWorkflow() {

        String addPayload = "{\"x\" : 10, \"y\" : \"test\"}";
        String id = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(addPayload)
                .when()
                .post("/samples/user-tasks")
                .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue())
                .extract().path("id");

        List<Map<String, String>> taskInfo = given()
                .accept(ContentType.JSON)
                .when()
                .get("/samples/user-tasks/" + id + "/tasks?user=john")
                .then()

                .statusCode(200)
                .extract().as(List.class);

        assertEquals(1, taskInfo.size());

        String taskId = taskInfo.get(0).get("id");
        String taskName = taskInfo.get(0).get("name");

        assertEquals("first-task", taskName);

        String payload = "{\"value\" : \"task completed\"}";
        given().contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(payload)
                .when()
                .post("/samples/user-tasks/" + id + "/" + taskName + "/" + taskId + "?user=john")
                .then()
                .log().body(true)
                .statusCode(200);

        taskInfo = given()
                .accept(ContentType.JSON)
                .when()
                .get("/samples/user-tasks/" + id + "/tasks?user=john")
                .then()

                .statusCode(200)
                .extract().as(List.class);

        assertEquals(1, taskInfo.size());

        taskId = taskInfo.get(0).get("id");
        taskName = taskInfo.get(0).get("name");

        assertEquals("second-task", taskName);

        payload = "{}";
        given().contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(payload)
                .when()
                .post("/samples/user-tasks/" + id + "/" + taskName + "/" + taskId + "?user=john")
                .then()
                .statusCode(200);

        given()
                .accept(ContentType.JSON)
                .when()
                .get("/samples/user-tasks")
                .then().statusCode(200)
                .body("$.size()", is(0));
    }
}
