
package io.automatiko.engine.api.jobs;

public interface JobDescription {

	String id();

	ExpirationTime expirationTime();

	Integer priority();
}
