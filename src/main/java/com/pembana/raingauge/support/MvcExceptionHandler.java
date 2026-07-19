package com.pembana.raingauge.support;

import jakarta.servlet.http.HttpServletResponse;

import com.pembana.raingauge.station.StationNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class MvcExceptionHandler {

	@ExceptionHandler(StationNotFoundException.class)
	String stationNotFound(StationNotFoundException exception, Model model,
			HttpServletResponse response) {
		response.setStatus(HttpStatus.NOT_FOUND.value());
		model.addAttribute("title", "Station not found");
		model.addAttribute("message", exception.getMessage());
		return "error";
	}

	@ExceptionHandler(IllegalArgumentException.class)
	String invalidRequest(IllegalArgumentException exception, Model model,
			HttpServletResponse response) {
		response.setStatus(HttpStatus.BAD_REQUEST.value());
		model.addAttribute("title", "Invalid request");
		model.addAttribute("message", exception.getMessage());
		return "error";
	}

}
