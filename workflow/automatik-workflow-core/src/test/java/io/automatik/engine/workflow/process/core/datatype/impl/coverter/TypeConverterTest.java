
package io.automatik.engine.workflow.process.core.datatype.impl.coverter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.jupiter.api.Test;

import io.automatik.engine.workflow.base.core.datatype.impl.type.ObjectDataType;

public class TypeConverterTest {

	@Test
	public void testStringObjectDataType() {

		ObjectDataType data = new ObjectDataType(java.lang.String.class);
		// no converted is used
		String readValue = (String) data.readValue("hello");
		assertEquals("hello", readValue);
	}

	@Test
	public void testDateObjectDataType() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

		Date now = new Date();

		ObjectDataType data = new ObjectDataType(java.util.Date.class);
		// date converted is used
		Date readValue = (Date) data.readValue(sdf.format(now));
		assertEquals(now.toString(), readValue.toString());
	}
}
