package com.pembana.raingauge.config;

import java.net.http.HttpClient;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ProviderRestClientFactory {

	private final RestClient.Builder builder;

	private final RainfallProperties properties;

	public ProviderRestClientFactory(RestClient.Builder builder, RainfallProperties properties) {
		this.builder = builder;
		this.properties = properties;
	}

	public RestClient create(String baseUrl) {
		RainfallProperties.Providers provider = this.properties.getProviders();
		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(provider.getConnectTimeout())
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(provider.getReadTimeout());
		return this.builder.clone()
				.baseUrl(baseUrl)
				.requestFactory(requestFactory)
				.defaultHeader(HttpHeaders.USER_AGENT, provider.getUserAgent())
				.defaultHeader(HttpHeaders.ACCEPT_ENCODING, "gzip")
				.build();
	}

}
