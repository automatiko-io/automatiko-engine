package io.automatiko.engine.api;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(TYPE)
public @interface Workflows {

    /**
     * Configures custom category that workflows of this class should belong to - allows to group workflows under same category
     * Only used when no category is specified on the workflow itself
     * 
     * @return this category
     */
    String category() default "";

    /**
     * When category is defined additional description can be provided to allow more information about the workflows in the
     * category
     * Only used when no category is specified
     * 
     * @return description of the category
     */
    String categoryDescription() default "";

    /**
     * Path that workflows of this class should be exposed by. This applies to Rest API of the workflow being created at build
     * time
     * Must follow <code>/element1/element2</code> format
     * 
     * @return path prefix to be used
     */
    String resourcePathPrefix() default "";
}
