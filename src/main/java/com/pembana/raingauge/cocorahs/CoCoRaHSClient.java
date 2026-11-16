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

package com.pembana.raingauge.cocorahs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.pembana.raingauge.config.ProviderRestClientFactory;
import com.pembana.raingauge.config.RainfallProperties;
import com.pembana.raingauge.station.client.ProviderException;
import com.pembana.raingauge.support.ProviderStatusRegistry;

/**
 * Retrieves station-scoped observation exports directly from CoCoRaHS.
 * @author OpenAI Codex
 */
@Component
class CoCoRaHSClient {

	private final RestClient restClient;

	private final RainfallProperties.Providers properties;

	private final ProviderStatusRegistry status;

	private final Clock clock;

	CoCoRaHSClient(ProviderRestClientFactory factory, RainfallProperties properties,
			ProviderStatusRegistry status, Clock clock) {
		this.properties = properties.getProviders();
		this.restClient = factory.create(this.properties.getCocorahsBaseUrl());
		this.status = status;
		this.clock = clock;
	}

	ReportYear fetch(String station, int year, LocalDate through) {
		long started = System.nanoTime();
		try {
			List<DailyReport> reports = new ArrayList<>(fetch(station, year, through, false));
			reports.addAll(fetch(station, year, through, true));
			Instant fetchedAt = this.clock.instant();
			this.status.cocorahsReportsSucceeded(fetchedAt, (System.nanoTime() - started) / 1_000_000);
			return new ReportYear(List.copyOf(reports), fetchedAt);
		}
		catch (ProviderException ex) {
			this.status.cocorahsReportsFailed(this.clock.instant(), ex.getMessage());
			throw ex;
		}
	}

	private List<DailyReport> fetch(String station, int year, LocalDate through, boolean multiDay) {
		LocalDate from = LocalDate.of(year, 1, 1);
		String reportType = multiDay ? "MultiDay" : "Daily";
		int limit = this.properties.getMaximumPayloadBytes();
		for (int attempt = 0; attempt <= this.properties.getRetries(); attempt++) {
			try {
				return this.restClient.get()
						.uri((builder) -> builder.path("/cocorahs/export/exportreports.aspx")
								.queryParam("ReportType", reportType)
								.queryParam("Format", "XML")
								.queryParam("dtf", 3)
								.queryParam("State", "HI")
								.queryParam("Station", station)
								.queryParam("StartDate", from)
								.queryParam("EndDate", through).build())
						.exchange((request, response) -> {
							int statusCode = response.getStatusCode().value();
							if (statusCode == 429 || statusCode >= 500) {
								throw new RestClientException("CoCoRaHS export returned HTTP " + statusCode);
							}
							if (!response.getStatusCode().is2xxSuccessful()) {
								throw new ProviderException("CoCoRaHS export returned HTTP " + statusCode);
							}
							InputStream body = response.getBody();
							try (InputStream input = "gzip".equalsIgnoreCase(
									response.getHeaders().getFirst("Content-Encoding"))
											? new GZIPInputStream(body) : body) {
								byte[] bytes = input.readNBytes(limit + 1);
								if (bytes.length > limit) {
									throw new ProviderException("CoCoRaHS export exceeded the payload limit");
								}
								return parse(new ByteArrayInputStream(bytes), station, from, through,
										multiDay);
							}
						});
			}
			catch (RestClientException ex) {
				if (attempt == this.properties.getRetries()) {
					throw new ProviderException("Unable to retrieve CoCoRaHS reports", ex);
				}
				backOff(attempt);
			}
		}
		throw new ProviderException("Unable to retrieve CoCoRaHS reports");
	}

	private void backOff(int attempt) {
		try {
			Thread.sleep(this.properties.getRetryInitialBackoff().multipliedBy(1L << Math.min(attempt, 8)));
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new ProviderException("Interrupted while retrieving CoCoRaHS reports", ex);
		}
	}

	static List<DailyReport> parse(InputStream input, String station, LocalDate from, LocalDate to,
			boolean multiDay) {
		String tag = multiDay ? "MultiDayPrecipReport" : "DailyPrecipReport";
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			var builder = factory.newDocumentBuilder();
			builder.setErrorHandler(new DefaultHandler());
			Element root = builder.parse(input).getDocumentElement();
			if (!"Cocorahs".equals(root.getTagName())
					|| root.getElementsByTagName(tag + "s").getLength() != 1) {
				throw new ProviderException("CoCoRaHS returned an unexpected export format");
			}
			NodeList rows = root.getElementsByTagName(tag);
			List<DailyReport> reports = new ArrayList<>();
			for (int index = 0; index < rows.getLength(); index++) {
				Element row = (Element) rows.item(index);
				if (!station.equals(field(row, "StationNumber"))) {
					throw new ProviderException("CoCoRaHS export returned a different station");
				}
				DailyReport report = parseRow(row, multiDay);
				if (report.end().isBefore(from) || report.end().isAfter(to)) {
					throw new ProviderException("CoCoRaHS export returned a date outside the request");
				}
				reports.add(report);
			}
			return List.copyOf(reports);
		}
		catch (ParserConfigurationException | SAXException | IOException | IllegalArgumentException | DateTimeException ex) {
			throw new ProviderException("Unable to read CoCoRaHS " + tag + " export", ex);
		}
	}

	private static DailyReport parseRow(Element row, boolean multiDay) {
		LocalDateTime observation = multiDay ? dateTime(field(row, "EndDateTime"))
				: LocalDate.parse(field(row, "ObservationDate"))
						.atTime(LocalTime.parse(field(row, "ObservationTime")));
		LocalDate start = multiDay ? LocalDate.parse(field(row, "StartDate"))
				: observation.toLocalDate();
		if (start.isAfter(observation.toLocalDate())) {
			throw new IllegalArgumentException("Reversed report span");
		}
		String value = field(row, "TotalPrecipAmt");
		BigDecimal inches = switch (value) {
			case "T", "NA", "" -> null;
			default -> new BigDecimal(value);
		};
		if (inches != null && inches.signum() < 0) {
			throw new IllegalArgumentException("Negative precipitation");
		}
		return new DailyReport(start, observation, dateTime(field(row, "DateTimeStamp")),
				inches, "T".equals(value), multiDay);
	}

	private static LocalDateTime dateTime(String value) {
		return LocalDateTime.parse(value.replace(' ', 'T'));
	}

	private static String field(Element row, String name) {
		NodeList fields = row.getElementsByTagName(name);
		if (fields.getLength() != 1) {
			throw new IllegalArgumentException("Missing or duplicate CoCoRaHS field " + name);
		}
		return fields.item(0).getTextContent().strip();
	}

	record ReportYear(List<DailyReport> reports, Instant fetchedAt) {
	}

}
