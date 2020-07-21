package io.automatik.engine.workflow.util;

import java.util.regex.Pattern;

public class PatternConstants {
	public static final Pattern PARAMETER_MATCHER = Pattern.compile("#\\{([\\S|\\p{javaWhitespace}&&[^\\}]]+)\\}",
			Pattern.DOTALL);
	public static final Pattern SIMPLE_TIME_DATE_MATCHER = Pattern
			.compile("([+-])?\\s*((\\d+)[Ww])?\\s*((\\d+)[Dd])?\\s*((\\d+)[Hh])?\\s*((\\d+)[Mm])?\\s*((\\d+)[Ss])?");

}
