package io.automatik.engine.workflow.http;

public class AuthorizationInfo {

	private String accessToken;

	private String refreshToken;

	private String refreshUrl;

	private String clientId;

	private String clientSecret;

	private String scope;

	private long expiresAt;

	public AuthorizationInfo(String accessToken, String refreshToken, String refreshUrl, String clientId,
			String clientSecret, String scope, long expiresAt) {
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		this.refreshUrl = refreshUrl;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.scope = scope;
		this.expiresAt = expiresAt;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public String getRefreshUrl() {
		return refreshUrl;
	}

	public void setRefreshUrl(String refreshUrl) {
		this.refreshUrl = refreshUrl;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public long getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(long expiresAt) {
		this.expiresAt = expiresAt;
	}

	public boolean isExpired() {
		return System.currentTimeMillis() >= getExpiresAt();
	}
}
