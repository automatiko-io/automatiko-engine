
package io.automatik.engine.jobs.api;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

public class URIBuilder {

	private URIBuilder() {

	}

	/**
	 * Transform the given url String into an {@link URI} and inserts the default
	 * port in case it was not explicit set on the url String.
	 * 
	 * @param urlStr
	 * @return
	 */
	public static URI toURI(String urlStr) {
		try {
			final URL url = new URL(urlStr);
			final Integer port = Optional.of(url.getPort()).filter(p -> !p.equals(-1)).orElse(url.getDefaultPort());
			final URI uri = url.toURI();
			return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), port, uri.getPath(), uri.getQuery(),
					uri.getFragment());

		} catch (MalformedURLException | URISyntaxException e) {
			throw new IllegalArgumentException("Not valid URI: " + urlStr, e);
		}
	}
}
