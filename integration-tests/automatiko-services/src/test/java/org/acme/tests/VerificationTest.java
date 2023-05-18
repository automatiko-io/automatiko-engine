package org.acme.tests;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.vertx.ext.mail.MailMessage;
import jakarta.inject.Inject;

@QuarkusTest
public class VerificationTest {

 // @formatter:off
    @Inject
    MockMailbox mailbox;

    @BeforeEach
    void init() {
        mailbox.clear();
    }
  
    @SuppressWarnings("unchecked")
    @Test
    public void testProcessExecutionV2() {
        String key = "test";
        String addPayload = "{\n"
                + "  \"email\": \"john@email.com\",\n"
                + "  \"person\": {\n"
                + "    \"adult\": true,\n"
                + "    \"age\": 30,\n"
                + "    \"name\": \"john\"\n"
                + "  },\n"
                + "  \"resume\": {\n"
                + "    \"attributes\": {\n"
                + "    },\n"
                + "    \"content\": \"aGVsbG8=\",\n"
                + "    \"name\": \"test.txt\"\n"
                + "  },\n"
                + "  \"coverLetter\": {\n"
                + "    \"attributes\": {\n"
                + "    },\n"
                + "    \"content\": \"aGVsbG8=\",\n"
                + "    \"name\": \"cover-letter.pdf\"\n"
                + "  }\n"
                + "}";
        
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/notifications?businessKey=" + key)
            .then()
                //.log().body(true)
                .statusCode(200)
                .body("id", equalTo("test"), "extracted", nullValue());
        
        List<MailMessage> sent = mailbox.getMailMessagesSentTo("john@email.com");
        assertEquals(1, sent.size());
        MailMessage actual = sent.get(0);
        assertTrue(actual.getHtml().contains("<h1>Hello john</h1>"));
        assertEquals("Notification", actual.getSubject());
        assertEquals(1, actual.getAttachment().size());
        assertEquals("application/zip", actual.getAttachment().get(0).getContentType());
        assertEquals("documents.zip", actual.getAttachment().get(0).getName());
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/notifications")
        .then().statusCode(200)
            .body("$.size()", is(1));
        
        List<Map<String, String>> taskInfo = given()
                        .accept(ContentType.JSON)
                    .when()
                        .get("/notifications/" + key + "/tasks")
                    .then()
                        .statusCode(200)
                        .extract().as(List.class);
        
        assertEquals(1, taskInfo.size());
        
        String taskId = taskInfo.get(0).get("id");
        String taskName = taskInfo.get(0).get("name");
        
        assertEquals("validate", taskName);
        
        String payload = "{}";
        given().
            contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(payload)
        .when()
            .post("/notifications/" + key + "/" + taskName + "/" + taskId)
        .then()
            .statusCode(200).body("id", is(key), "extracted", notNullValue());              
        
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/notifications")
        .then().statusCode(200)
            .body("$.size()", is(0));        
    }
    
    
 // @formatter:on
}
