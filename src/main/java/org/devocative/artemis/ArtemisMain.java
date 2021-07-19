package org.devocative.artemis;

import com.thoughtworks.xstream.XStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.devocative.artemis.xml.*;
import org.devocative.artemis.xml.method.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static org.devocative.artemis.xml.XBaseRequest.EMethod.POST;

@Slf4j
public class ArtemisMain {

	public static void run() throws Exception {
		final XStream xStream = new XStream();
		XStream.setupDefaultSecurity(xStream);
		xStream.processAnnotations(new Class[]{XScenario.class, XGet.class, XPost.class, XPut.class, XDelete.class});
		xStream.allowTypesByWildcard(new String[]{"org.devocative.artemis.xml.**"});

		final XScenario scenario = (XScenario) xStream.fromXML(ArtemisMain.class.getResourceAsStream("/artemis.xml"));
		final XScenario proxy = (XScenario) Proxy.create(scenario);
		log.info("Scenario: {}", proxy.getName());

		for (XBaseRequest rq : proxy.getRequests()) {
			processRq(rq);
		}
	}

	private static void processRq(XBaseRequest rq) throws Exception {
		final XInit init = rq.getInit();
		if (init != null) {
			if (init.getCall() != null) {
				GroovyHandler.invoke(init.getCall());
			}
			final Context ctx = GroovyHandler.getContext();
			final List<XVar> vars = init.getVars();
			if (vars != null) {
				for (XVar var : vars) {
					ctx.addVar(var.getName(), var.getValue());
				}
			}
		}

		log.info("Rq - id({}): {}", rq.getId(), rq.getUrl());

		sendRq(rq);
	}

	private static void sendRq(XBaseRequest rq) throws Exception {
		final Context ctx = GroovyHandler.getContext();
		final String url = ctx.getBaseUrl() + rq.getUrl(); //TODO
		final List<XParam> params = rq.getParams();

		final URI uri;
		if (rq.getMethod() == POST || params == null) {
			uri = URI.create(url);
		} else {
			final URIBuilder builder = new URIBuilder(url);
			params.forEach(p -> builder.addParameter(p.getName(), p.getValue()));
			uri = builder.build();
		}

		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			final HttpUriRequestBase httpRq = new HttpUriRequestBase(rq.getMethod().name(), uri);

			final XBody body = rq.getBody();
			final StringBuilder builder = new StringBuilder();
			if (rq.getMethod() == POST) {
				if (body != null) {
					httpRq.setEntity(new StringEntity(body.getContent().trim(), ContentType.APPLICATION_JSON, "UTF-8", false));
					builder
						.append("\n")
						.append(body.getContent().trim());
				} else if (params != null) {
					final List<BasicNameValuePair> httpParams = params.stream()
						.map(p -> new BasicNameValuePair(p.getName(), p.getValue()))
						.collect(Collectors.toList());
					httpRq.setEntity(new UrlEncodedFormEntity(httpParams));

					builder.append("\n");
					params.forEach(p -> builder.append(p.getName()).append(" = ").append(p.getValue()));
				}
			}

			log.info("{} - {}{}", rq.getMethod(), uri, builder.toString());
			try (final CloseableHttpResponse rs = httpclient.execute(httpRq)) {
				final String contentType = rs.getEntity().getContentType();
				if (contentType.contains("text") || contentType.contains("json")) {
					final String rsBodyAsStr = new BufferedReader(new InputStreamReader(rs.getEntity().getContent(), StandardCharsets.UTF_8))
						.lines()
						.collect(Collectors.joining("\n"));

					log.info("{} ({}) - {}\nContentType: {}\n{}",
						rq.getMethod(), rs.getCode(), uri, contentType, rsBodyAsStr);
				}
			}
		}
	}

}
