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
public class VerificationSubprocessTest {
 // @formatter:off
    
    static {
        RestAssured.registerParser("application/json, application/javascript, text/javascript, text/json", Parser.JSON);
    }
    
    @Test
    public void testProcessNotVersionedPassThrough() {

        String addPayload = "{\"query\":\"mutation {create_collects(data: {name: \\\"john\\\"}) {id,name, scripts {id,name}}}\\n\",\"variables\":null}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/graphql")
            .then()
                //.log().all(true)
                .statusCode(200)
                .body("data.create_collects.id", notNullValue(), "data.create_collects.name", equalTo("john"), "data.create_collects.message", nullValue());
        
       String getInstances = "{\"query\":\"query {get_all_collects {id,name,scripts{id}}}\\n\",\"variables\":null}";
        
        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(getInstances)
        .when()
            .post("/graphql")
        .then()
            //.log().all(true)
            .statusCode(200)
            .body("data.get_all_collects.size()", is(0));
    
    }
    
    @Test
    public void testProcessAbortParent() {

        String addPayload = "{\"query\":\"mutation {create_collects(key: \\\"test\\\", data: {name: \\\"mary\\\"}) {id,name, scripts {id,name}}}\\n\",\"variables\":null}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/graphql")
            .then()
                //.log().all(true)
                .statusCode(200)
                .body("data.create_collects.id", notNullValue(), "data.create_collects.name", equalTo("mary"), "data.create_collects.scripts[0].id", notNullValue());
        
       String getInstances = "{\"query\":\"query {get_all_collects {id,name,scripts{id}}}\\n\",\"variables\":null}";
        
        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(getInstances)
        .when()
            .post("/graphql")
        .then()
            //.log().all(true)
            .statusCode(200)
            .body("data.get_all_collects.size()", is(1));
        
       String getChildInstances = "{\"query\":\"query {get_all_scripts {id,name}}\\n\",\"variables\":null}";
        
        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(getChildInstances)
        .when()
            .post("/graphql")
        .then()
            //.log().all(true)
            .statusCode(200)
            .body("data.get_all_scripts.size()", is(1));
        
        String removePayload = "{\"query\":\"mutation {delete_collects(id: \\\"test\\\") {id,name}}\\n\",\"variables\":null}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(removePayload)
            .when()
                .post("/graphql")
            .then()
                //.log().all(true)
                .statusCode(200)
                .body("data.delete_collects.id", notNullValue(), "data.delete_collects.name", equalTo("mary"), "data.delete_collects.message", nullValue());
        
        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(getInstances)
        .when()
            .post("/graphql")
        .then()
            //.log().all(true)
            .statusCode(200)
            .body("data.get_all_collects.size()", is(0));
    
        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(getChildInstances)
        .when()
            .post("/graphql")
        .then()
            //.log().all(true)
            .statusCode(200)
            .body("data.get_all_scripts.size()", is(0));
    }
    
    @Test
    public void testProcessAbortChild() {

        String addPayload = "{\"query\":\"mutation {create_collects(key: \\\"test\\\", data: {name: \\\"mary\\\"}) {id,name, scripts {id,name}}}\\n\",\"variables\":null}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/graphql")
            .then()
                //.log().all(true)
                .statusCode(200)
                .body("data.create_collects.id", notNullValue(), "data.create_collects.name", equalTo("mary"), "data.create_collects.scripts[0].id", notNullValue());
        
       String getInstances = "{\"query\":\"query {get_all_collects {id,name,scripts{id}}}\\n\",\"variables\":null}";
        
       String parentId = given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(getInstances)
        .when()
            .post("/graphql")
        .then()
            //.log().all(true)
            .statusCode(200)
            .body("data.get_all_collects.size()", is(1))
            .extract().path("data.get_all_collects[0].id");
        
       String getChildInstances = "{\"query\":\"query {get_all_scripts {id,name}}\\n\",\"variables\":null}";
        
        String childId = given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(getChildInstances)
        .when()
            .post("/graphql")
        .then()
            //.log().all(true)
            .statusCode(200)
            .body("data.get_all_scripts.size()", is(1))
            .extract().path("data.get_all_scripts[0].id");
        
        String removePayload = "{\"query\":\"mutation {collects_delete_scripts(parentId: \\\"" + parentId + "\\\", id: \\\"" + childId + "\\\") {id,name}}\\n\",\"variables\":null}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(removePayload)
            .when()
                .post("/graphql")
            .then()
                //.log().all(true)
                .statusCode(200)
                .body("data.collects_delete_scripts.id", notNullValue(), "data.collects_delete_scripts.name", equalTo("mary"), "data.collects_delete_scripts.message", nullValue());
        
        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(getInstances)
        .when()
            .post("/graphql")
        .then()
            //.log().all(true)
            .statusCode(200)
            .body("data.get_all_collects.size()", is(0));
    
        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(getChildInstances)
        .when()
            .post("/graphql")
        .then()
            //.log().all(true)
            .statusCode(200)
            .body("data.get_all_scripts.size()", is(0));
    }
    
    
    @Test
    public void testProcessCompleteViaTask() {

        String addPayload = "{\"query\":\"mutation {create_collects(key: \\\"test\\\", data: {name: \\\"mary\\\"}) {id,name, scripts {id,name}}}\\n\",\"variables\":null}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/graphql")
            .then()
                //.log().all(true)
                .statusCode(200)
                .body("data.create_collects.id", notNullValue(), "data.create_collects.name", equalTo("mary"), "data.create_collects.scripts[0].id", notNullValue());
        
       String getInstances = "{\"query\":\"query {get_all_collects {id,name,scripts{id}}}\\n\",\"variables\":null}";
        
       String parentId = given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(getInstances)
        .when()
            .post("/graphql")
        .then()
            //.log().all(true)
            .statusCode(200)
            .body("data.get_all_collects.size()", is(1))
            .extract().path("data.get_all_collects[0].id");
        
       String getChildInstances = "{\"query\":\"query {get_all_scripts {id,name}}\\n\",\"variables\":null}";
        
        String childId = given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(getChildInstances)
        .when()
            .post("/graphql")
        .then()
            //.log().all(true)
            .statusCode(200)
            .body("data.get_all_scripts.size()", is(1))
            .extract().path("data.get_all_scripts[0].id");
        
        String getTasks = "{\"query\":\"query {get_scripts_tasks(id: \\\"" + parentId+":"+childId + "\\\", user: \\\"john\\\") {id,name}}\\n\",\"variables\":null}";
        
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
 
        String completeTaskPayload = "{\"query\":\"mutation {completeTask_approval_0(id: \\\"" + parentId+":"+childId + "\\\", workItemId: \\\"" + taskId +"\\\", user:\\\"john\\\", data: {}) {id,name}}\\n\",\"variables\":null}";
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
            .body("data.get_all_collects.size()", is(0));
    
        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(getChildInstances)
        .when()
            .post("/graphql")
        .then()
            //.log().all(true)
            .statusCode(200)
            .body("data.get_all_scripts.size()", is(0));
    }
    
    @Test
    public void testProcessCompleteViaSignal() {

        String addPayload = "{\"query\":\"mutation {create_collects(key: \\\"test\\\", data: {name: \\\"mike\\\"}) {id,name, scripts {id,name}}}\\n\",\"variables\":null}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(addPayload)
            .when()
                .post("/graphql")
            .then()
                //.log().all(true)
                .statusCode(200)
                .body("data.create_collects.id", notNullValue(), "data.create_collects.name", equalTo("mike"), "data.create_collects.scripts[0].id", notNullValue());
        
       String getInstances = "{\"query\":\"query {get_all_collects {id,name,scripts{id}}}\\n\",\"variables\":null}";
        
       String parentId = given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(getInstances)
        .when()
            .post("/graphql")
        .then()
            //.log().all(true)
            .statusCode(200)
            .body("data.get_all_collects.size()", is(1))
            .extract().path("data.get_all_collects[0].id");
        
       String getChildInstances = "{\"query\":\"query {get_all_scripts {id,name}}\\n\",\"variables\":null}";
        
        String childId = given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(getChildInstances)
        .when()
            .post("/graphql")
        .then()
            //.log().all(true)
            .statusCode(200)
            .body("data.get_all_scripts.size()", is(1))
            .extract().path("data.get_all_scripts[0].id");
        
 
        String signalPayload = "{\"query\":\"mutation {collects_signal_update_0(parentId: \\\"" + parentId + "\\\", id: \\\"" + childId +"\\\", user:\\\"john\\\", model: \\\"updated message\\\") {id,name,message}}\\n\",\"variables\":null}";
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(signalPayload)
            .when()
                .post("/graphql")
            .then()
                //.log().all(true)
                .statusCode(200)
                .body("data.collects_signal_update_0.id", notNullValue(), "data.collects_signal_update_0.name", equalTo("mike"), "data.collects_signal_update_0.message", equalTo("updated message"));
        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(getInstances)
        .when()
            .post("/graphql")
        .then()
            //.log().all(true)
            .statusCode(200)
            .body("data.get_all_collects.size()", is(0));
    
        given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(getChildInstances)
        .when()
            .post("/graphql")
        .then()
            //.log().all(true)
            .statusCode(200)
            .body("data.get_all_scripts.size()", is(0));
    }
    
    
 // @formatter:on
}
