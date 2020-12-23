
package io.automatiko.engine.quarkus.ittests.bpmn;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.http.ContentType;

public class HotReloadTest {

    private static final String PACKAGE = "io.automatiko.engine.quarkus.ittests";
    private static final String PACKAGE_FOLDER = PACKAGE.replace('.', '/');
    private static final String RESOURCE_FILE = PACKAGE_FOLDER + "/text-process.bpmn";
    private static final String ORDERS_RESOURCE_FILE = PACKAGE_FOLDER + "/orders.bpmn";
    private static final String HTTP_TEST_PORT = "65535";

    @RegisterExtension
    final static QuarkusDevModeTest test = new QuarkusDevModeTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class).addAsResource("orders.txt", ORDERS_RESOURCE_FILE)
                    .addAsResource("orderItems.txt", PACKAGE_FOLDER + "/orderItems.bpmn")
                    .addAsResource("CalculationService.txt", PACKAGE_FOLDER + "/CalculationService.java")
                    .addAsResource("Order.txt", PACKAGE_FOLDER + "/Order.java")
                    .addAsResource("OrdersProcessService.txt", PACKAGE_FOLDER + "/OrdersProcessService.java")
                    .addAsResource("text-process.txt", RESOURCE_FILE)
                    .addAsResource("JbpmHotReloadTestHelper.txt", PACKAGE_FOLDER + "/JbpmHotReloadTestHelper.java"));

    @SuppressWarnings("unchecked")
    @Test
    public void testServletChange() {

        String payload = "{\"mytext\": \"HeLlO\"}";

        Map<String, String> result = given().baseUri("http://localhost:" + HTTP_TEST_PORT).contentType(ContentType.JSON)
                .accept(ContentType.JSON).body(payload).when().post("/text_process").then().statusCode(200).extract()
                .as(Map.class);

        assertEquals(2, result.size());
        assertEquals("HELLO", result.get("mytext"));

        test.modifyResourceFile(RESOURCE_FILE, s -> s.replaceAll("toUpper", "toLower"));

        result = given().baseUri("http://localhost:" + HTTP_TEST_PORT).contentType(ContentType.JSON)
                .accept(ContentType.JSON).body(payload).when().post("/text_process").then().statusCode(200).extract()
                .as(Map.class);

        assertEquals(2, result.size());
        assertEquals("hello", result.get("mytext"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOrdersProcessChange() {

        String payload = "{\"approver\": \"John\", \"order\" : {\"orderNumber\" : \"12345\"}}";

        Map<String, String> result = given().baseUri("http://localhost:" + HTTP_TEST_PORT).contentType(ContentType.JSON)
                .accept(ContentType.JSON).body(payload).when().post("/v1_0/orders").then().statusCode(200).extract()
                .as(Map.class);

        assertEquals(3, result.size());
        assertEquals("JOHN", result.get("approver"));

        List<?> orderItems = given().baseUri("http://localhost:" + HTTP_TEST_PORT).contentType(ContentType.JSON)
                .accept(ContentType.JSON).when().get("/v1_0/orders/" + result.get("id") + "/orderItems").then()
                .statusCode(200)
                .extract()
                .as(List.class);

        assertEquals(1, orderItems.size());

        test.modifyResourceFile(ORDERS_RESOURCE_FILE, s -> s.replaceAll("toUpper", "toLower"));

        result = given().baseUri("http://localhost:" + HTTP_TEST_PORT).contentType(ContentType.JSON)
                .accept(ContentType.JSON).body(payload).when().post("/v1_0/orders").then().statusCode(200).extract()
                .as(Map.class);

        assertEquals(3, result.size());
        assertEquals("john", result.get("approver"));

        orderItems = given().baseUri("http://localhost:" + HTTP_TEST_PORT).contentType(ContentType.JSON)
                .accept(ContentType.JSON).when().get("/v1_0/orders/" + result.get("id") + "/orderItems").then()
                .statusCode(200)
                .extract()
                .as(List.class);

        assertEquals(1, orderItems.size());
    }
}
