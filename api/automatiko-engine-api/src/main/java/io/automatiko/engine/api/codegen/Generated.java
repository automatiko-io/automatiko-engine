
package io.automatiko.engine.api.codegen;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ TYPE })
public @interface Generated {
	/**
	 * The value element MUST have the name of the code generator.
	 */
	String[] value();

	/**
	 * A reference identifier that the generated class refers to
	 * 
	 * @return reference identifier
	 */
	String reference();

	/**
	 * A optional name to be used
	 * 
	 * @return alternative name
	 */
	String name() default "";

	/**
	 * Optional flag indicating that the generated class shall be hidden from other
	 * generators.
	 * 
	 * @return true if the class should be hidden otherwise false
	 */
	boolean hidden() default false;
}