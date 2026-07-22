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

package com.pembana.raingauge.support;

import jakarta.servlet.http.HttpServletRequest;

import com.pembana.raingauge.config.RainfallProperties;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Adds canonical page metadata to MVC models.
 * @author Gunnar Hillert
 */
@ControllerAdvice
public class PageMetadataControllerAdvice {

	private final String baseUrl;

	/**
	 * Creates a new {@code PageMetadataControllerAdvice}.
	 * @param properties the rainfall application properties
	 */
	PageMetadataControllerAdvice(RainfallProperties properties) {
		this.baseUrl = withoutTrailingSlash(properties.getSite().getBaseUrl().toString());
	}

	/**
	 * Adds canonical page metadata to every MVC model.
	 * @param request the request
	 * @return the resulting page metadata
	 */
	@ModelAttribute("pageMetadata")
	PageMetadata pageMetadata(HttpServletRequest request) {
		return new PageMetadata(this.baseUrl + request.getRequestURI(),
				this.baseUrl + "/social-image.png");
	}

	/**
	 * Removes a trailing slash from a URL.
	 * @param value the value
	 * @return the resulting without trailing slash
	 */
	private static String withoutTrailingSlash(String value) {
		return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
	}

}
