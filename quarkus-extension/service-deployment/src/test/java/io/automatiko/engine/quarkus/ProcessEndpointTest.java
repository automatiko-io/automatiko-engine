package io.automatiko.engine.quarkus;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasKey;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.automatiko.engine.api.Application;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.http.ContentType;

public class ProcessEndpointTest {

	@RegisterExtension
	static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
			.create(JavaArchive.class).addAsResource("test-process.bpmn", "src/main/resources/test-process.bpmn")
			.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

	@Inject
	Application application;

	@Test
	public void testProcessRestEndpoint() {

		given().body("{}").contentType(ContentType.JSON).when().post("/tests").then().statusCode(200).body("$",
				hasKey("id"));
	}
}
