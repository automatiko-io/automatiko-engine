
package io.automatik.engine.api.codegen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface VariableInfo {
	/**
	 * Variable tags assigned to given property of the model
	 */
	String tags() default "";

}