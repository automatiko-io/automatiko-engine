package org.acme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Any;
import javax.inject.Inject;
import javax.mail.Message;
import javax.mail.MessagingException;

import org.apache.camel.Exchange;
import org.apache.camel.component.mail.MailMessage;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.reactive.messaging.camel.CamelMessage;
import io.smallrye.reactive.messaging.connectors.InMemoryConnector;
import io.smallrye.reactive.messaging.connectors.InMemorySource;

@QuarkusTest
public class VerificationTest {
 // @formatter:off
    
    @Inject 
    @Any
    InMemoryConnector connector;

    
    @Test
    public void testProcessExecution() throws MessagingException {
        Exchange exchange = Mockito.mock(Exchange.class);        
        MailMessage mail = Mockito.mock(MailMessage.class);
        Message originalMail = Mockito.mock(Message.class);
        
        Mockito.when(mail.getOriginalMessage()).thenReturn(originalMail);
        Mockito.when(originalMail.getSubject()).thenReturn("Important Message");
        
        Mockito.when(mail.getBody(Mockito.any())).thenReturn("Hello");
        Mockito.when(mail.getExchange()).thenReturn(exchange);
        
        Mockito.when(exchange.getIn()).thenReturn(mail);
        
        InMemorySource<CamelMessage<?>> channelT = connector.source("email-received");               
        
        CamelMessage<Object> message = new CamelMessage<Object>(exchange, null);
        
        channelT.send(message);
   
        String id = given()
            .accept(ContentType.JSON)
        .when()
            .get("/emails")
        .then().statusCode(200)            
            .body("$.size()", is(1))
            .extract().path("[0].id");
        
        Map data = given()
                .accept(ContentType.JSON)
            .when()
                .get("/emails/" + id)
            .then()
                .statusCode(200).body("email", notNullValue()).extract().as(Map.class);
        
        Map<String, Object> email = (Map<String, Object>) data.get("email");
        
        assertEquals("Important Message", email.get("subject"));
        assertEquals("Hello", email.get("body"));
                
        List<Map<String, String>> taskInfo = given()
                .accept(ContentType.JSON)
            .when()
                .get("/emails/" + id + "/tasks")
            .then()
                .statusCode(200)
                .extract().as(List.class);

        assertEquals(1, taskInfo.size());
        
        String taskId = taskInfo.get(0).get("id");
        String taskName = taskInfo.get(0).get("name");
        
        assertEquals("Task_Name", taskName);
        
        String payload = "{}";
        given().
            contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(payload)
        .when()
            .post("/emails/" + id + "/" + taskName + "/" + taskId)
        .then()
            .statusCode(200).body("id", is(id), "email", notNullValue());
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/emails")
        .then().statusCode(200)
            .body("$.size()", is(0));
        
        File dumpFolder = new File("target/emails");
        
        assertEquals(1, dumpFolder.listFiles().length);
        
        for (File f : dumpFolder.listFiles()) {
            f.delete();
        }        
    }
    
    @Test
    public void testProcessThrowIntermediateEvent() {

        String addPayload = "{\"content\" : \"hello john\"}";
        given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .body(addPayload)
                    .when()
                        .post("/files")
                    .then()
                        //.log().body(true)
                        .statusCode(200)
                        .body("id", notNullValue(), "content", equalTo("hello john"));
        
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/files")
        .then().statusCode(200)
            .body("$.size()", is(0));
        
        File dumpFolder = new File("target/files");
        
        assertEquals(1, dumpFolder.listFiles().length);
        
        for (File f : dumpFolder.listFiles()) {
            f.delete();
        }  
    }
    
 // @formatter:on
}
