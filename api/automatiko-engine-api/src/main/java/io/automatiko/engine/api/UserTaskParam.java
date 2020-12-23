package io.automatiko.engine.api;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(FIELD)
public @interface UserTaskParam {

	String VALUE_PARAM = "value";

	ParamType value();

	enum ParamType {
		INPUT, OUTPUT
	}
}
