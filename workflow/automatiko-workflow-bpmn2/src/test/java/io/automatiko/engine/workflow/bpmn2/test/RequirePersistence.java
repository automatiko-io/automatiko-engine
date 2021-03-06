
package io.automatiko.engine.workflow.bpmn2.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface RequirePersistence {

	boolean value() default true;

	String comment() default "";

}
