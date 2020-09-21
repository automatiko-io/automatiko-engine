package io.automatik.engine.workflow.http;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class HttpAuthorization {

	private static HttpAuthorization INSTANCE = new HttpAuthorization();

	private HttpClient httpcClient;
	private Map<String, AuthorizationInfo> authorizations = new ConcurrentHashMap<>();

	private HttpAuthorization() {
		this.httpcClient = HttpClient.newBuilder().version(Version.HTTP_2).followRedirects(Redirect.NORMAL).build();
	}

	public String token(String id, String clientId, String clientSecret, String refreshToken, String refreshUrl,
			String scope, Function<InputStream, Map<String, String>> mapper) {

		if (authorizations.containsKey(id)) {
			AuthorizationInfo info = authorizations.get(id);

			if (!info.isExpired()) {
				return info.getAccessToken();
			}
		}

		try {

			StringBuilder body = new StringBuilder();
			body.append("grant_type=refresh_token&client_id=").append(clientId).append("&scope=").append(scope)
					.append("&client_secret=").append(clientSecret).append("&refresh_token=" + refreshToken);

			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(refreshUrl))
					.header("Content-Type", "application/x-www-form-urlencoded")
					.POST(BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8)).build();

			HttpResponse<InputStream> response = httpcClient.send(request, BodyHandlers.ofInputStream());

			if (response.statusCode() < 300) {
				Map<String, String> data = mapper.apply(response.body());

				String accessToken = data.get("access_token");
				String expires = data.get("expires_in");
				String rToken = data.get("refresh_token");

				long expiresAt = System.currentTimeMillis() + Long.parseLong(expires);

				authorizations.put(id, new AuthorizationInfo(accessToken, rToken, refreshUrl, clientId, clientSecret,
						scope, expiresAt));

				return accessToken;

			} else {
				throw new RuntimeException("Could not refresh OAUTH token. " + "[" + response.statusCode() + "] ");
			}
		} catch (Exception e) {
			throw new RuntimeException("Unable to get access token", e);
		}
	}

	public static HttpAuthorization get() {
		return INSTANCE;
	}
}
