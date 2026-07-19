package com.pembana.raingauge.support;

import java.net.URI;

import com.pembana.raingauge.rainfall.UnsupportedRainfallStationException;
import com.pembana.raingauge.station.StationNotFoundException;
import com.pembana.raingauge.station.client.ProviderException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler {

	@ExceptionHandler(StationNotFoundException.class)
	ProblemDetail stationNotFound(StationNotFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, "Station not found", exception.getMessage(),
				"station-not-found");
	}

	@ExceptionHandler(UnsupportedRainfallStationException.class)
	ProblemDetail unsupported(UnsupportedRainfallStationException exception) {
		return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Rainfall data unsupported",
				exception.getMessage(), "rainfall-unsupported");
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ProblemDetail invalid(IllegalArgumentException exception) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage(),
				"invalid-request");
	}

	@ExceptionHandler(ProviderException.class)
	ProblemDetail provider(ProviderException exception) {
		return problem(HttpStatus.SERVICE_UNAVAILABLE, "Observation provider unavailable",
				exception.getMessage(), "provider-unavailable");
	}

	private ProblemDetail problem(HttpStatus status, String title, String detail, String type) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		problem.setType(URI.create("https://pembana.com/problems/" + type));
		return problem;
	}

}
