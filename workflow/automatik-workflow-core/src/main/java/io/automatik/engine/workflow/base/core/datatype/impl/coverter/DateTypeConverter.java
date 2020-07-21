
package io.automatik.engine.workflow.base.core.datatype.impl.coverter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Function;

public class DateTypeConverter implements Function<String, Date> {

	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

	@Override
	public Date apply(String t) {
		try {
			return sdf.parse(t);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

}
