package io.automatiko.engine.quarkus;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.automatiko.engine.api.Application;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.http.ContentType;

public class DecisionProcessEndpointTest {

	@RegisterExtension
	static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
			.create(JavaArchive.class).addAsResource("dmnprocess.bpmn", "src/main/resources/dmnprocess.bpmn")
			.addAsResource("vacationDays.dmn", "src/main/resources/vacationDays.dmn")
			.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

	@Inject
	Application application;

	@Test
	public void testProcessRestEndpoint() {

		given().body("{\"age\":16, \"yearsOfService\":1}").contentType(ContentType.JSON).when().post("/v1_0/DmnProcess")
				.then().statusCode(200).body("$", hasKey("id"), "vacationDays", equalTo(27));
	}
}
