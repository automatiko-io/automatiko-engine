package org.acme.files;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class ExportImportVerificationTest {
 // @formatter:off
    
    @SuppressWarnings("unchecked")
    @Test
    public void testProcessNotVersionedWithInitiator() {

        String addPayload = "{\n"
                + "  \"file\": {\n"
                + "    \"content\": \"Y29udGVudA==\",\n"
                + "    \"name\": \"test.txt\"\n"
                + "  }\n"
                + "}}";
        String id = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(addPayload)
                .when()
                    .post("/v1_0/files")
                .then()
                    //.log().body(true)
                    .statusCode(200)
                    .body("id", notNullValue(), "file", notNullValue(), "file.content", nullValue(), "file.url", notNullValue())
                    .extract().path("id");
            
            given()
                .accept(ContentType.JSON)
            .when()
                .get("/v1_0/files")
            .then().statusCode(200)
                .body("$.size()", is(1));
        
        // now let's export that instance and abort it so it can be successfully imported back
        JsonNode exported = given()
            .accept(ContentType.JSON)
        .when()
            .get("/management/processes/files_1_0/instances/" + id + "/export?abort=true")
        .then()
            .statusCode(200)
            .extract().as(JsonNode.class);  
        
        assertNotNull(exported);
        // after export with abort no instance should be present               
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/v1_0/files")
        .then().statusCode(200)
            .body("$.size()", is(0));
        
        // now, import it back and continue with execution
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(exported.toString())
        .when()
            .post("/management/processes/files_1_0/instances")
        .then()
                //.log().body(true)
            .statusCode(200)
            .body("id", notNullValue());
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/v1_0/files/" + id)
        .then()
            .statusCode(200)
            .body("id", notNullValue(), "file", notNullValue(), "file.content", nullValue(), "file.url", notNullValue());
        
        List<Map<String, String>> taskInfo = given()
                .accept(ContentType.JSON)
            .when()
                .get("/v1_0/files/" + id + "/tasks")
            .then()
                .statusCode(200)
                .extract().as(List.class);

        assertEquals(1, taskInfo.size());
        
        String taskId = taskInfo.get(0).get("id");
        String taskName = taskInfo.get(0).get("name");
        
        assertEquals("approve", taskName);
        
        String fileDownloadUrl = given()
            .accept(ContentType.JSON)
        .when()
            .get("/v1_0/files/" + id + "/" + taskName + "/" + taskId)
        .then()
            .statusCode(200)
            .body("file", notNullValue(), "file.content", nullValue(), "file.url", notNullValue())
            .extract().path("file.url");
        
        given()
            .accept("*/*")
        .when()
            .get(fileDownloadUrl)
        .then()
            .statusCode(200)
            .contentType("text/plain")
            .header("Content-Disposition", "attachment;filename=test.txt");
        
        String payload = "{\"approved\" : true}";
        given().
            contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(payload)
        .when()
            .post("/v1_0/files/" + id + "/" + taskName + "/" + taskId)
        .then()
            .statusCode(200).body("id", is(id), "approved", notNullValue());        
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/v1_0/files")
        .then().statusCode(200)
            .body("$.size()", is(0));     
    }
 // @formatter:on
}
