package org.acme.files;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class VerificationTest {
 // @formatter:off
   
    
    @SuppressWarnings("unchecked")
    @Test
    public void testProcessSingleFileHandling() {

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
        .log().body(true)
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
    
    @SuppressWarnings("unchecked")
    @Test
    public void testProcessFileListHandling() {

        String addPayload = "{\n"
                + "  \"documents\": [\n"
                + "    {\n"
                + "    \"content\": \"Y29udGVudA==\",\n"
                + "    \"name\": \"one.txt\"\n"
                + "  },\n"
                + "{\n"
                + "    \"content\": \"Y29udGVudA==\",\n"
                + "    \"name\": \"two.txt\"\n"
                + "  },\n"
                + "{\n"
                + "    \"content\": \"Y29udGVudA==\",\n"
                + "    \"name\": \"three.txt\"\n"
                + "  }\n"
                + "  ]\n"
                + "}";
        String id = given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/filelist")
            .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue(), "documents.size()", is(3), "documents[0].content", nullValue(), "documents[0].url", notNullValue()
                        , "documents[1].content", nullValue(), "documents[1].url", notNullValue()
                        , "documents[2].content", nullValue(), "documents[2].url", notNullValue())
                .extract().path("id");
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/filelist")
        .then().statusCode(200)
            .body("$.size()", is(1));
        
        List<Map<String, String>> taskInfo = given()
                .accept(ContentType.JSON)
            .when()
                .get("/filelist/" + id + "/tasks")
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
            .get("/filelist/" + id + "/" + taskName + "/" + taskId)
        .then()
            .statusCode(200)
            .body("files.size()", is(3), "files[0].content", nullValue(), "files[0].url", notNullValue()
                    , "files[1].content", nullValue(), "files[1].url", notNullValue()
                    , "files[2].content", nullValue(), "files[2].url", notNullValue())
            .extract().path("files[0].url");
            
            given()
                .accept("*/*")
            .when()
                .get(fileDownloadUrl)
            .then()
                .statusCode(200)
                .contentType("text/plain")
                .header("Content-Disposition", "attachment;filename=one.txt");
             
        
        String payload = "{\"approved\" : true}";
        given().
            contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(payload)
        .when()
            .post("/filelist/" + id + "/" + taskName + "/" + taskId)
        .then()
            .statusCode(200).body("id", is(id), "approved", notNullValue());        
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/filelist")
        .then().statusCode(200)
            .body("$.size()", is(0));
    }
    
 // @formatter:on
}
