package org.acme.travels.archive;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

@QuarkusTest
@TestProfile(ArchiveInstanceTestProfile.class)
public class VerificationTest {
 // @formatter:off
    
    @Test
    public void testProcessNotVersioned() {

        String addPayload = "{\"name\" : \"john\"}";
        String id = given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/scripts")
            .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", notNullValue(), "name", equalTo("john"), "message", equalTo("Hello john"), "lastName", nullValue())
                .extract().path("id");
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/scripts")
        .then().statusCode(200)
            .body("$.size()", is(0));
        
        File archiveFolder = new File("target", "archives");
        assertTrue(archiveFolder.exists());
        
        File archiveFile = new File(archiveFolder, "scripts" + File.separator + id +".zip");
        assertTrue(archiveFile.exists());
    }
    
 // @formatter:on
}
