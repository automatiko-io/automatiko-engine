
package io.automatik.engine.services.identity;

import java.util.Collections;
import java.util.List;

import io.automatik.engine.api.auth.IdentityProvider;

/**
 * Simple implementation of identity provider that must always be used for just
 * one security context, needs to be recreated every time for each "request"
 *
 * Relies on given name and roles
 */
public class StaticIdentityProvider implements IdentityProvider {

	private String name;
	private List<String> roles;

	public StaticIdentityProvider(String name) {
		this(name, Collections.emptyList());
	}

	public StaticIdentityProvider(String name, List<String> roles) {
		this.name = name;
		this.roles = roles == null ? Collections.emptyList() : roles;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public List<String> getRoles() {
		return roles;
	}

	@Override
	public boolean hasRole(String role) {
		return roles.contains(role);
	}

}
