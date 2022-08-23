package org.devocative.artemis.http;

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.client5.http.routing.HttpRoutePlanner;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.net.URIBuilder;
import org.devocative.artemis.TestFailedException;
import org.devocative.artemis.log.ALog;
import org.devocative.artemis.xml.XBaseRequest;

import java.io.IOException;
import java.net.*;

public class HttpFactory {
	private static final ThreadLocal<CloseableHttpClient> CURRENT_CLIENT = new ThreadLocal<>();

	private final String baseUrl;
	private final String proxy;

	// ------------------------------

	public HttpFactory(String baseUrl, String proxy) {
		this.baseUrl = baseUrl;
		this.proxy = proxy;
	}

	// ------------------------------

	public void shutdown() {
		try {
			final CloseableHttpClient httpclient = CURRENT_CLIENT.get();
			if (httpclient != null) {
				httpclient.close();
			}
			CURRENT_CLIENT.remove();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public HttpRequest create(XBaseRequest rq) {
		final String url = rq.getUrl();

		final String finalUrl;
		if (url.startsWith("http://") || url.startsWith("https://")) {
			finalUrl = url;
		} else if (!url.matches("^[/?].*")) {
			throw new TestFailedException(rq.getId(), "Invalid URL: '%s'", url);
		} else {
			finalUrl = baseUrl + url;
		}

		final URI uri;
		if (rq.getUrlParams().isEmpty()) {
			uri = URI.create(finalUrl);
		} else {
			try {
				final URIBuilder builder = new URIBuilder(finalUrl);
				rq.getUrlParams().forEach(param -> builder.addParameter(param.getName(), param.getValue()));
				uri = builder.build();
			} catch (URISyntaxException e) {
				throw new TestFailedException(rq.getId(), "Invalid URI to Build");
			}
		}

		final CloseableHttpClient httpclient;
		if (CURRENT_CLIENT.get() == null) {
			if (proxy != null) {
				final String[] scheme_other = proxy.split("://");
				final String proxyScheme = scheme_other[0];
				final String[] host_port = scheme_other[1].split(":");
				final String proxyHost = host_port[0];
				final int proxyPort = Integer.parseInt(host_port[1]);

				if ("http".equals(proxyScheme)) {
					final HttpRoutePlanner proxyRoutePlanner = new DefaultProxyRoutePlanner(new HttpHost(proxyScheme, proxyHost, proxyPort));
					httpclient = HttpClients
						.custom()
						.setRoutePlanner(proxyRoutePlanner)
						.build();
				} else if ("socks".equals(proxyScheme)) {
					final Registry<ConnectionSocketFactory> reg = RegistryBuilder
						.<ConnectionSocketFactory>create()
						.register("http", new ProxiedConnectionSocketFactory(proxyHost, proxyPort))
						.build();

					final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(reg);
					httpclient = HttpClients
						.custom()
						.setConnectionManager(cm)
						.build();
				} else {
					throw new RuntimeException("Invalid proxy scheme: " + proxyScheme);
				}
			} else {
				httpclient = HttpClients.createDefault();
			}
			CURRENT_CLIENT.set(httpclient);
		} else {
			httpclient = CURRENT_CLIENT.get();
		}

		return new HttpRequest(rq.getId(), rq.getGlobalId(), new HttpUriRequestBase(rq.getMethod().name(), uri), httpclient);
	}

	// ------------------------------

	static class ProxiedConnectionSocketFactory extends PlainConnectionSocketFactory {
		private final String proxyHost;
		private final int proxyPort;

		public ProxiedConnectionSocketFactory(String proxyHost, int proxyPort) {
			this.proxyHost = proxyHost;
			this.proxyPort = proxyPort;
		}

		@Override
		public Socket createSocket(HttpContext context) {
			ALog.info("ProxiedConnectionSocketFactory: socks={}:{}", proxyHost, proxyPort);
			final InetSocketAddress socksAddr = new InetSocketAddress(proxyHost, proxyPort);
			final Proxy proxy = new Proxy(Proxy.Type.SOCKS, socksAddr);
			return new Socket(proxy);
		}
	}
}
