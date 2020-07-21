
package io.automatik.engine.addons.persistence.infinispan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.junit.jupiter.api.Test;

import io.automatik.engine.addons.persistence.infinispan.ProtoStreamObjectMarshallingStrategy;

public class ProtoStreamObjectMarshallingStrategyTest {

	private ProtoStreamObjectMarshallingStrategy protoStreamMarshallerStrategy = new ProtoStreamObjectMarshallingStrategy(
			null);

	@Test
	public void testStringMarshalling() throws Exception {

		String value = "here is simple string value";

		boolean accepted = protoStreamMarshallerStrategy.accept(value);
		assertTrue(accepted, "String type should be accepted");

		byte[] data = protoStreamMarshallerStrategy.marshal(null, null, value);
		assertNotNull(data, "Marshalled content should not be null");

		Object returned = protoStreamMarshallerStrategy.unmarshal("kogito.String", null, null, data,
				this.getClass().getClassLoader());
		assertNotNull(returned, "Unmarshalled value should not be null");
		assertEquals(value, returned, "Values should be the same");

	}

	@Test
	public void testIntegerMarshalling() throws Exception {

		Integer value = 25;

		boolean accepted = protoStreamMarshallerStrategy.accept(value);
		assertTrue(accepted, "Integer type should be accepted");

		byte[] data = protoStreamMarshallerStrategy.marshal(null, null, value);
		assertNotNull(data, "Marshalled content should not be null");

		Object returned = protoStreamMarshallerStrategy.unmarshal("kogito.Integer", null, null, data,
				this.getClass().getClassLoader());
		assertNotNull(returned, "Unmarshalled value should not be null");
		assertEquals(value, returned, "Values should be the same");

	}

	@Test
	public void testLongMarshalling() throws Exception {

		Long value = 555L;

		boolean accepted = protoStreamMarshallerStrategy.accept(value);
		assertTrue(accepted, "Long type should be accepted");

		byte[] data = protoStreamMarshallerStrategy.marshal(null, null, value);
		assertNotNull(data, "Marshalled content should not be null");

		Object returned = protoStreamMarshallerStrategy.unmarshal("kogito.Long", null, null, data,
				this.getClass().getClassLoader());
		assertNotNull(returned, "Unmarshalled value should not be null");
		assertEquals(value, returned, "Values should be the same");

	}

	@Test
	public void testDoubleMarshalling() throws Exception {

		Double value = 55.5;

		boolean accepted = protoStreamMarshallerStrategy.accept(value);
		assertTrue(accepted, "Double type should be accepted");

		byte[] data = protoStreamMarshallerStrategy.marshal(null, null, value);
		assertNotNull(data, "Marshalled content should not be null");

		Object returned = protoStreamMarshallerStrategy.unmarshal("kogito.Double", null, null, data,
				this.getClass().getClassLoader());
		assertNotNull(returned, "Unmarshalled value should not be null");
		assertEquals(value, returned, "Values should be the same");

	}

	@Test
	public void testFloatMarshalling() throws Exception {

		Float value = 55.5f;

		boolean accepted = protoStreamMarshallerStrategy.accept(value);
		assertTrue(accepted, "Float type should be accepted");

		byte[] data = protoStreamMarshallerStrategy.marshal(null, null, value);
		assertNotNull(data, "Marshalled content should not be null");

		Object returned = protoStreamMarshallerStrategy.unmarshal("kogito.Float", null, null, data,
				this.getClass().getClassLoader());
		assertNotNull(returned, "Unmarshalled value should not be null");
		assertEquals(value, returned, "Values should be the same");

	}

	@Test
	public void testBooleanMarshalling() throws Exception {

		Boolean value = true;

		boolean accepted = protoStreamMarshallerStrategy.accept(value);
		assertTrue(accepted, "Boolean type should be accepted");

		byte[] data = protoStreamMarshallerStrategy.marshal(null, null, value);
		assertNotNull(data, "Marshalled content should not be null");

		Object returned = protoStreamMarshallerStrategy.unmarshal("kogito.Boolean", null, null, data,
				this.getClass().getClassLoader());
		assertNotNull(returned, "Unmarshalled value should not be null");
		assertEquals(value, returned, "Values should be the same");

	}

	@Test
	public void testDateMarshalling() throws Exception {

		Date value = new Date();

		boolean accepted = protoStreamMarshallerStrategy.accept(value);
		assertTrue(accepted, "Date type should be accepted");

		byte[] data = protoStreamMarshallerStrategy.marshal(null, null, value);
		assertNotNull(data, "Marshalled content should not be null");

		Object returned = protoStreamMarshallerStrategy.unmarshal("kogito.Date", null, null, data,
				this.getClass().getClassLoader());
		assertNotNull(returned, "Unmarshalled value should not be null");
		assertEquals(value, returned, "Values should be the same");

	}
}
