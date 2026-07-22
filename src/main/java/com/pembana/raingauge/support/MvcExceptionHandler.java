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

import jakarta.servlet.http.HttpServletResponse;

import com.pembana.raingauge.station.StationNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Handles MVC exception.
 * @author Gunnar Hillert
 */
@ControllerAdvice
public class MvcExceptionHandler {

	/** Creates the MVC exception handler. */
	public MvcExceptionHandler() {
	}

	/**
	 * Renders the not-found page for an unknown station.
	 * @param exception the exception to translate
	 * @param model the MVC model to populate
	 * @param response the response
	 * @return the error template name
	 */
	@ExceptionHandler(StationNotFoundException.class)
	String stationNotFound(StationNotFoundException exception, Model model,
			HttpServletResponse response) {
		response.setStatus(HttpStatus.NOT_FOUND.value());
		model.addAttribute("title", "Station not found");
		model.addAttribute("message", exception.getMessage());
		return "error";
	}

	/**
	 * Creates an invalid-request problem response.
	 * @param exception the exception to translate
	 * @param model the MVC model to populate
	 * @param response the response
	 * @return the resulting invalid request
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	String invalidRequest(IllegalArgumentException exception, Model model,
			HttpServletResponse response) {
		response.setStatus(HttpStatus.BAD_REQUEST.value());
		model.addAttribute("title", "Invalid request");
		model.addAttribute("message", exception.getMessage());
		return "error";
	}

}
