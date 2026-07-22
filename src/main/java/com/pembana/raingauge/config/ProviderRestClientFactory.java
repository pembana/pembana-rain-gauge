/*
 * Copyright 2026 Gunnar Hillert
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pembana.raingauge.config;

import java.net.http.HttpClient;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Creates provider rest client instances.
 * @author Gunnar Hillert
 */
@Component
public class ProviderRestClientFactory {

	private final RestClient.Builder builder;

	private final RainfallProperties properties;

	/**
	 * Creates a new {@code ProviderRestClientFactory}.
	 * @param builder the preconfigured REST client builder
	 * @param properties the rainfall application properties
	 */
	public ProviderRestClientFactory(RestClient.Builder builder, RainfallProperties properties) {
		this.builder = builder;
		this.properties = properties;
	}

	/**
	 * Creates a REST client for the supplied provider base URL.
	 * @param baseUrl the base URL
	 * @return the created instance
	 */
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
