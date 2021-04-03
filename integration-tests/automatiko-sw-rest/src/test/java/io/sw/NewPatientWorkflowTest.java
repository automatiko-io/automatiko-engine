package io.sw;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class NewPatientWorkflowTest {

    @Test
    public void testNewPatientWorkflow() throws Exception {
        String addPayload = "{\"identifier\": \"123\", \"name\": \"John\", \"condition\": \"bladder infection\"}";

        String workflowData = given()
                .contentType("application/json")
                .accept("application/json")
                .body(addPayload)
                .when()
                .post("/newpatient")
                .then()
                .statusCode(200)
                .body("id", notNullValue()).extract().asString();

        assertNotNull(workflowData);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode responseNode = mapper.readTree(workflowData);
        assertNotNull(responseNode.get("doctor"));

    }

    @Test
    public void testNewPatientWorkflowErrorRetries() throws Exception {
        String addPayload = "{\"identifier\": \"123\", \"name\": \"John\"}";

        String id = given()
                .contentType("application/json")
                .accept("application/json")
                .body(addPayload)
                .when()
                .post("/newpatient")
                .then()
                .log().body(true)
                .statusCode(200)
                .body("id", notNullValue())
                .extract().path("id");

        long elapsed = 3000;

        while (elapsed >= 0) {
            elapsed -= 1000;
            Thread.sleep(1000);
            int size = given()
                    .accept(ContentType.JSON)
                    .when()
                    .get("/newpatient")
                    .then().statusCode(200)
                    .extract().path("$.size()");

            if (size == 0) {
                break;
            }
        }

    }
}
