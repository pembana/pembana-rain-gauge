package com.pembana.raingauge.support;

import jakarta.servlet.http.HttpServletRequest;

import com.pembana.raingauge.config.RainfallProperties;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class PageMetadataControllerAdvice {

	private final String baseUrl;

	PageMetadataControllerAdvice(RainfallProperties properties) {
		this.baseUrl = withoutTrailingSlash(properties.getSite().getBaseUrl().toString());
	}

	@ModelAttribute("pageMetadata")
	PageMetadata pageMetadata(HttpServletRequest request) {
		return new PageMetadata(this.baseUrl + request.getRequestURI(),
				this.baseUrl + "/social-image.png");
	}

	private static String withoutTrailingSlash(String value) {
		return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
	}

}
