package org.devocative.artemis.http;

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.devocative.artemis.TestFailedException;
import org.devocative.artemis.log.ALog;
import org.devocative.artemis.xml.XBaseRequest;

import javax.net.ssl.SSLContext;
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
			final SocketProxy socketProxy = new SocketProxy(proxy);

			final Registry<ConnectionSocketFactory> reg = RegistryBuilder
				.<ConnectionSocketFactory>create()
				.register("http", new ProxiedConnectionSocketFactory(socketProxy))
				.register("https", new ProxiedSslConnectionSocketFactory(socketProxy))
				.build();

			final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(reg);

			final HttpClientBuilder clientBuilder = HttpClients
				.custom()
				.setConnectionManager(cm);

			if (socketProxy.isSet() && socketProxy.isHttp()) {
				clientBuilder.setRoutePlanner(new DefaultProxyRoutePlanner(socketProxy.createHttpProxy()));
			}

			httpclient = clientBuilder.build();

			CURRENT_CLIENT.set(httpclient);
		} else {
			httpclient = CURRENT_CLIENT.get();
		}

		return new HttpRequest(rq.getId(), rq.getGlobalId(), new HttpUriRequestBase(rq.getMethod().name(), uri), httpclient);
	}

	// ------------------------------

	static class SocketProxy {
		private final String scheme;
		private final String host;
		private final int port;

		public SocketProxy(String proxy) {
			if (proxy != null) {
				final String[] scheme_other = proxy.split("://");
				this.scheme = scheme_other[0].toLowerCase();
				final String[] host_port = scheme_other[1].split(":");
				this.host = host_port[0];
				this.port = Integer.parseInt(host_port[1]);
			} else {
				this.scheme = null;
				this.host = null;
				this.port = -1;
			}
		}

		public boolean isSet() {
			return host != null;
		}

		public boolean isSocks() {
			return "socks".equals(scheme);
		}

		public boolean isHttp() {
			return "http".equals(scheme);
		}

		public Proxy createSocksProxy() {
			final InetSocketAddress socksAddr = new InetSocketAddress(host, port);
			return new Proxy(Proxy.Type.SOCKS, socksAddr);
		}

		public HttpHost createHttpProxy() {
			return new HttpHost(scheme, host, port);
		}

		@Override
		public String toString() {
			return String.format("%s://%s:%s", scheme, host, port);
		}
	}

	static class ProxiedConnectionSocketFactory extends PlainConnectionSocketFactory {
		private final SocketProxy socketProxy;

		public ProxiedConnectionSocketFactory(SocketProxy socketProxy) {
			this.socketProxy = socketProxy;
		}

		@Override
		public Socket createSocket(HttpContext context) throws IOException {
			if (socketProxy.isSet() && socketProxy.isSocks()) {
				ALog.info("ProxiedConnectionSocketFactory: {}", socketProxy);
				return new Socket(socketProxy.createSocksProxy());
			} else {
				return super.createSocket(context);
			}
		}
	}

	static class ProxiedSslConnectionSocketFactory extends SSLConnectionSocketFactory {
		private final SocketProxy socketProxy;


		public ProxiedSslConnectionSocketFactory(SocketProxy socketProxy) {
			super(createSslSocketFactory(), NoopHostnameVerifier.INSTANCE);
			this.socketProxy = socketProxy;
		}

		@Override
		public Socket createSocket(HttpContext context) throws IOException {
			if (socketProxy.isSet() && socketProxy.isSocks()) {
				ALog.info("ProxiedSslConnectionSocketFactory: {}", socketProxy);
				return new Socket(socketProxy.createSocksProxy());
			} else {
				return super.createSocket(context);
			}
		}

		private static SSLContext createSslSocketFactory() {
			try {
				final TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
				return SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}
