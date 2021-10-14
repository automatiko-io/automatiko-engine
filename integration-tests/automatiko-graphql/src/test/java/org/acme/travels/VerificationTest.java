package org.acme.travels;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;

@QuarkusTest
public class VerificationTest {
 // @formatter:off
    
    static {
        RestAssured.registerParser("application/json, application/javascript, text/javascript, text/json", Parser.JSON);
    }
    
    @Test
    public void testProcessNotVersionedPassThrough() {

        String addPayload = "{\"query\":\"mutation {create_scripts(data: {name: \\\"john\\\"}) {id,name}}\\n\",\"variables\":null}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/graphql")
            .then()
                //.log().all(true)
                .statusCode(200)
                .body("data.create_scripts.id", notNullValue(), "data.create_scripts.name", equalTo("john"), "data.create_scripts.message", nullValue());
        
        addPayload = "{\"query\":\"mutation {create_scripts(data: {name: \\\"john\\\"}) {id,name,message}}\\n\",\"variables\":null}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/graphql")
            .then()
                //.log().all(true)
                .statusCode(200)
                .body("data.create_scripts.id", notNullValue(), "data.create_scripts.name", equalTo("john"), "data.create_scripts.message", equalTo("Hello john"));
        
        String getInstances = "{\"query\":\"query {get_all_scripts {id,name,message}}\\n\",\"variables\":null}";
        
        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(getInstances)
        .when()
            .post("/graphql")
        .then()
            //.log().all(true)
            .statusCode(200)
            .body("data.get_all_scripts.size()", is(0));
    
    }
    
    @Test
    public void testProcessNotVersionedAbort() {

        String addPayload = "{\"query\":\"mutation {create_scripts(key: \\\"test\\\", data: {name: \\\"mary\\\"}) {id,name}}\\n\",\"variables\":null}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/graphql")
            .then()
                //.log().all(true)
                .statusCode(200)
                .body("data.create_scripts.id", notNullValue(), "data.create_scripts.name", equalTo("mary"), "data.create_scripts.message", nullValue());
        
       
        String getInstances = "{\"query\":\"query {get_all_scripts(user: \\\"john\\\") {id,name,message}}\\n\",\"variables\":null}";
        
        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(getInstances)
        .when()
            .post("/graphql")
        .then()
            //.log().all(true)
            .statusCode(200)
            .body("data.get_all_scripts.size()", is(1));
 
        String removePayload = "{\"query\":\"mutation {delete_scripts(id: \\\"test\\\") {id,name}}\\n\",\"variables\":null}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(removePayload)
            .when()
                .post("/graphql")
            .then()
                //.log().all(true)
                .statusCode(200)
                .body("data.delete_scripts.id", notNullValue(), "data.delete_scripts.name", equalTo("mary"), "data.delete_scripts.message", nullValue());
        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(getInstances)
        .when()
            .post("/graphql")
        .then()
            //.log().all(true)
            .statusCode(200)
            .body("data.get_all_scripts.size()", is(0));
         
    }
    
    @Test
    public void testProcessNotVersionedCompleteViaTask() {

        String addPayload = "{\"query\":\"mutation {create_scripts(key: \\\"test\\\", data: {name: \\\"mary\\\"}) {id,name}}\\n\",\"variables\":null}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/graphql")
            .then()
                //.log().all(true)
                .statusCode(200)
                .body("data.create_scripts.id", notNullValue(), "data.create_scripts.name", equalTo("mary"), "data.create_scripts.message", nullValue());
        
       
        String getInstances = "{\"query\":\"query {get_all_scripts(user: \\\"john\\\") {id,name,message}}\\n\",\"variables\":null}";
        
        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(getInstances)
        .when()
            .post("/graphql")
        .then()
            //.log().all(true)
            .statusCode(200)
            .body("data.get_all_scripts.size()", is(1));
        
        String getTasks = "{\"query\":\"query {get_scripts_tasks(id: \\\"test\\\", user: \\\"john\\\") {id,name}}\\n\",\"variables\":null}";
        
        String taskId = given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(getTasks)
        .when()
            .post("/graphql")
        .then()
            //.log().all(true)
            .statusCode(200)
            .body("data.get_scripts_tasks.size()", is(1))
            .extract().path("data.get_scripts_tasks[0].id");
 
        String completeTaskPayload = "{\"query\":\"mutation {completeTask_approval_0(id: \\\"test\\\", workItemId: \\\"" + taskId +"\\\", user:\\\"john\\\", data: {}) {id,name}}\\n\",\"variables\":null}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(completeTaskPayload)
            .when()
                .post("/graphql")
            .then()
                //.log().all(true)
                .statusCode(200)
                .body("data.completeTask_approval_0.id", notNullValue(), "data.completeTask_approval_0.name", equalTo("mary"), "data.completeTask_approval_0.message", nullValue());
        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(getInstances)
        .when()
            .post("/graphql")
        .then()
            //.log().all(true)
            .statusCode(200)
            .body("data.get_all_scripts.size()", is(0));
         
    }
    
    @Test
    public void testProcessNotVersionedAbortViaTask() {

        String addPayload = "{\"query\":\"mutation {create_scripts(key: \\\"test\\\", data: {name: \\\"mary\\\"}) {id,name}}\\n\",\"variables\":null}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/graphql")
            .then()
                //.log().all(true)
                .statusCode(200)
                .body("data.create_scripts.id", notNullValue(), "data.create_scripts.name", equalTo("mary"), "data.create_scripts.message", nullValue());
        
       
        String getInstances = "{\"query\":\"query {get_all_scripts(user: \\\"john\\\") {id,name,message}}\\n\",\"variables\":null}";
        
        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(getInstances)
        .when()
            .post("/graphql")
        .then()
            //.log().all(true)
            .statusCode(200)
            .body("data.get_all_scripts.size()", is(1));
        
        String getTasks = "{\"query\":\"query {get_scripts_tasks(id: \\\"test\\\", user: \\\"john\\\") {id,name}}\\n\",\"variables\":null}";
        
        String taskId = given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(getTasks)
        .when()
            .post("/graphql")
        .then()
            //.log().all(true)
            .statusCode(200)
            .body("data.get_scripts_tasks.size()", is(1))
            .extract().path("data.get_scripts_tasks[0].id");
 
        String abortTaskPayload = "{\"query\":\"mutation {abortTask_approval_0(id: \\\"test\\\", workItemId: \\\"" + taskId +"\\\", user:\\\"john\\\", phase: \\\"skip\\\") {id,name}}\\n\",\"variables\":null}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(abortTaskPayload)
            .when()
                .post("/graphql")
            .then()
                //.log().all(true)
                .statusCode(200)
                .body("data.abortTask_approval_0.id", notNullValue(), "data.abortTask_approval_0.name", equalTo("mary"), "data.abortTask_approval_0.message", nullValue());
      
        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(getInstances)
        .when()
            .post("/graphql")
        .then()
            //.log().all(true)
            .statusCode(200)
            .body("data.get_all_scripts.size()", is(0));
         
    }
    
    @Test
    public void testProcessNotVersionedCompleteViaSignal() {

        String addPayload = "{\"query\":\"mutation {create_scripts(key: \\\"test\\\", data: {name: \\\"mike\\\"}) {id,name}}\\n\",\"variables\":null}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/graphql")
            .then()
                //.log().all(true)
                .statusCode(200)
                .body("data.create_scripts.id", notNullValue(), "data.create_scripts.name", equalTo("mike"), "data.create_scripts.message", nullValue());
        
       
        String getInstances = "{\"query\":\"query {get_all_scripts(user: \\\"john\\\") {id,name,message}}\\n\",\"variables\":null}";
        
        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(getInstances)
        .when()
            .post("/graphql")
        .then()
            //.log().all(true)
            .statusCode(200)
            .body("data.get_all_scripts.size()", is(1));
        
        String signalPayload = "{\"query\":\"mutation {signal_update_0(id: \\\"test\\\", user:\\\"john\\\", model: \\\"updated message\\\") {id,name,message}}\\n\",\"variables\":null}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(signalPayload)
            .when()
                .post("/graphql")
            .then()
                //.log().all(true)
                .statusCode(200)
                .body("data.signal_update_0.id", notNullValue(), "data.signal_update_0.name", equalTo("mike"), "data.signal_update_0.message", equalTo("updated message"));
     
        
        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(getInstances)
        .when()
            .post("/graphql")
        .then()
            //.log().all(true)
            .statusCode(200)
            .body("data.get_all_scripts.size()", is(0));
         
    }
    
    @Test
    public void testProcessDuplicatedBussinesKey() {

        String addPayload = "{\"query\":\"mutation {create_scripts(key: \\\"test\\\", data: {name: \\\"mary\\\"}) {id,name}}\\n\",\"variables\":null}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/graphql")
            .then()
                //.log().all(true)
                .statusCode(200)
                .body("data.create_scripts.id", notNullValue(), "data.create_scripts.name", equalTo("mary"), "data.create_scripts.message", nullValue());
        
        
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/graphql")
            .then()
                //.log().all(true)
                .statusCode(200)
                .body("data.create_scripts", nullValue(), "errors.size()", is(1), "errors[0].message", equalTo("Process instance with id 'test' already exists, usually this means business key has been already used"));
        
        String removePayload = "{\"query\":\"mutation {delete_scripts(id: \\\"test\\\") {id,name}}\\n\",\"variables\":null}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(removePayload)
            .when()
                .post("/graphql")
            .then()
                //.log().all(true)
                .statusCode(200)
                .body("data.delete_scripts.id", notNullValue(), "data.delete_scripts.name", equalTo("mary"), "data.delete_scripts.message", nullValue());
        
        String getInstances = "{\"query\":\"query {get_all_scripts(user: \\\"john\\\") {id,name,message}}\\n\",\"variables\":null}";
        
        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(getInstances)
        .when()
            .post("/graphql")
        .then()
            //.log().all(true)
            .statusCode(200)
            .body("data.get_all_scripts.size()", is(0));
       
    }
 // @formatter:on
}
