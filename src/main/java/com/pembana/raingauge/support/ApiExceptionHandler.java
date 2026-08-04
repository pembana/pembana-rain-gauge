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

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.pembana.raingauge.rainfall.UnsupportedRainfallStationException;
import com.pembana.raingauge.station.StationNotFoundException;
import com.pembana.raingauge.station.client.ProviderException;

/**
 * Handles API exception.
 * @author Gunnar Hillert
 */
@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler {

	/**
	 * Creates a problem response for an unknown station.
	 * @param exception the exception to translate
	 * @return the not-found problem detail
	 */
	@ExceptionHandler(StationNotFoundException.class)
	ProblemDetail stationNotFound(StationNotFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, "Station not found", exception.getMessage(),
				"station-not-found");
	}

	/**
	 * Handles requests for stations without rainfall capability.
	 * @param exception the exception to translate
	 * @return the resulting unsupported
	 */
	@ExceptionHandler(UnsupportedRainfallStationException.class)
	ProblemDetail unsupported(UnsupportedRainfallStationException exception) {
		return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Rainfall data unsupported",
				exception.getMessage(), "rainfall-unsupported");
	}

	/**
	 * Handles invalid request arguments.
	 * @param exception the exception to translate
	 * @return the resulting invalid
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	ProblemDetail invalid(IllegalArgumentException exception) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage(),
				"invalid-request");
	}

	/**
	 * Handles upstream provider failures.
	 * @param exception the exception to translate
	 * @return the resulting provider
	 */
	@ExceptionHandler(ProviderException.class)
	ProblemDetail provider(ProviderException exception) {
		return problem(HttpStatus.SERVICE_UNAVAILABLE, "Observation provider unavailable",
				exception.getMessage(), "provider-unavailable");
	}

	/**
	 * Creates an RFC 9457 problem detail response.
	 * @param status the status
	 * @param title the title
	 * @param detail the detail
	 * @param type the type
	 * @return the resulting problem
	 */
	private ProblemDetail problem(HttpStatus status, String title, String detail, String type) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		problem.setType(URI.create("https://pembana.com/problems/" + type));
		return problem;
	}

}
