package io.automatiko.engine.api;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(TYPE)
public @interface UserTask {

	String TASK_NAME_PARAM = "taskName";
	String PROCESS_NAME_PARAM = "processName";

	String taskName();

	String processName();
}
