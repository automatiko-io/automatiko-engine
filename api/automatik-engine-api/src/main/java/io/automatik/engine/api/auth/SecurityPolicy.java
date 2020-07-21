
package io.automatik.engine.api.auth;

import java.util.Objects;

import io.automatik.engine.api.workflow.workitem.Policy;

/**
 * Security policy that delivers <code>IdentityProvider</code> to allow to
 * security related policy enforcement.
 *
 */
public class SecurityPolicy implements Policy<IdentityProvider> {

	private IdentityProvider identity;

	/**
	 * Creates new SecurityPolicy for given identity provider
	 * 
	 * @param identity non null identity provider
	 * @return new instance of SecurityPolicy
	 */
	public static SecurityPolicy of(IdentityProvider identity) {
		Objects.requireNonNull(identity);
		return new SecurityPolicy(identity);
	}

	protected SecurityPolicy(IdentityProvider identity) {
		this.identity = identity;
	}

	@Override
	public IdentityProvider value() {
		return identity;
	}

}
