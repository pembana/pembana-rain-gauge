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

package com.pembana.raingauge.observation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Parses HADS observation payloads.
 * @author Gunnar Hillert
 */
@Component
public class HadsObservationParser {

	/** Creates the HADS observation parser. */
	public HadsObservationParser() {
	}

	private static final DateTimeFormatter IEM_TIMESTAMP =
			DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");

	/**
	 * Parses precipitation observations from an IEM HADS response.
	 * @param body the provider response body
	 * @param requestedShefKey the requested SHEF variable key
	 * @return the parsed result
	 */
	public ObservationParseResult parse(String body, String requestedShefKey) {
		List<String> lines = body.lines().filter((line) -> !line.isBlank()).toList();
		if (lines.isEmpty()) {
			return new ObservationParseResult(List.of(), List.of("Provider response was empty"), 0, 0);
		}
		List<String> header = parseCsvLine(lines.getFirst());
		Map<String, Integer> columns = indexColumns(header);
		Integer stationColumn = first(columns, "station", "station_id", "nwsli");
		Integer validColumn = first(columns, "utc_valid", "valid", "valid_at");
		if (stationColumn == null || validColumn == null) {
			return new ObservationParseResult(List.of(),
					List.of("Required station and utc_valid columns were not present"), 0,
					Math.max(0, lines.size() - 1));
		}
		List<VariableColumn> variableColumns = findVariables(header, requestedShefKey);
		if (variableColumns.isEmpty()) {
			return new ObservationParseResult(List.of(),
					List.of("Requested SHEF variable " + requestedShefKey + " was not present"),
					0, Math.max(0, lines.size() - 1));
		}
		List<PrecipitationObservation> observations = new ArrayList<>();
		List<String> warnings = new ArrayList<>();
		int parsedRows = 0;
		int rejectedRows = 0;
		for (int lineIndex = 1; lineIndex < lines.size(); lineIndex++) {
			List<String> values = parseCsvLine(lines.get(lineIndex));
			try {
				String stationId = required(values, stationColumn, "station");
				Instant validAt = timestamp(required(values, validColumn, "utc_valid"));
				boolean rowHadValue = false;
				for (VariableColumn variable : variableColumns) {
					String text = value(values, variable.index());
					if (text == null || text.isBlank()) {
						continue;
					}
					BigDecimal amount = new BigDecimal(text);
					String qualifier = value(values, columns.get("qualifier"));
					ObservationQuality quality = quality(qualifier);
					Integer receivedColumn = first(columns, "utc_received", "received");
					Instant receivedAt = timestampOrNull(value(values, receivedColumn));
					observations.add(new PrecipitationObservation(stationId, validAt,
							receivedAt, variable.normalizedKey(), variable.sourceKey(),
							amount, quality, qualifier,
							value(values, first(columns, "source", "source_code")),
							value(values, first(columns, "unit", "units")), lineIndex));
					rowHadValue = true;
				}
				if (rowHadValue) {
					parsedRows++;
				}
			}
			catch (RuntimeException ex) {
				rejectedRows++;
				warnings.add("Row " + (lineIndex + 1) + " rejected: " + ex.getMessage());
			}
		}
		return new ObservationParseResult(observations, warnings, parsedRows, rejectedRows);
	}

	/**
	 * Indexes provider response columns by normalized name.
	 * @param header the header
	 * @return the resulting index columns
	 */
	private Map<String, Integer> indexColumns(List<String> header) {
		Map<String, Integer> columns = new HashMap<>();
		for (int index = 0; index < header.size(); index++) {
			columns.put(header.get(index).strip().toLowerCase(Locale.ROOT), index);
		}
		return columns;
	}

	/**
	 * Finds precipitation variable columns in a provider header.
	 * @param header the header
	 * @param requestedShefKey the requested SHEF variable key
	 * @return the matching variables
	 */
	private List<VariableColumn> findVariables(List<String> header, String requestedShefKey) {
		String normalizedRequested = normalizeShefKey(requestedShefKey);
		List<VariableColumn> variables = new ArrayList<>();
		for (int index = 0; index < header.size(); index++) {
			String sourceKey = header.get(index).strip().toUpperCase(Locale.ROOT);
			String normalized = normalizeShefKey(sourceKey);
			if (normalized.equals(normalizedRequested)) {
				variables.add(new VariableColumn(index, sourceKey, normalized));
			}
		}
		return variables;
	}

	/**
	 * Normalizes a SHEF variable key for comparison.
	 * @param key the key
	 * @return the resulting normalize SHEF key
	 */
	private String normalizeShefKey(String key) {
		String normalized = key.strip().toUpperCase(Locale.ROOT);
		return normalized.endsWith("ZZ") ? normalized.substring(0, normalized.length() - 2)
				: normalized;
	}

	/**
	 * Returns the first available column index for the candidate names.
	 * @param columns the columns
	 * @param names the names
	 * @return the resulting first
	 */
	private @Nullable Integer first(Map<String, Integer> columns, String... names) {
		for (String name : names) {
			Integer column = columns.get(name);
			if (column != null) {
				return column;
			}
		}
		return null;
	}

	/**
	 * Returns a required field value from a parsed response row.
	 * @param values the values
	 * @param index the zero-based value index
	 * @param field the field
	 * @return the required field value
	 */
	private String required(List<String> values, int index, String field) {
		String value = value(values, index);
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " was missing");
		}
		return value;
	}

	/**
	 * Returns this rainfall amount in the requested unit.
	 * @param values the values
	 * @param index the zero-based value index
	 * @return the resulting value
	 */
	private @Nullable String value(List<String> values, @Nullable Integer index) {
		return (index != null && index >= 0 && index < values.size()) ? values.get(index).strip() : null;
	}

	/**
	 * Parses a required provider timestamp.
	 * @param value the value
	 * @return the resulting timestamp
	 */
	private Instant timestamp(String value) {
		try {
			return Instant.parse(value);
		}
		catch (DateTimeParseException ex) {
			try {
				return LocalDateTime.parse(value, IEM_TIMESTAMP).toInstant(ZoneOffset.UTC);
			}
			catch (DateTimeParseException nested) {
				throw new IllegalArgumentException("timestamp was invalid", nested);
			}
		}
	}

	/**
	 * Parses an optional provider timestamp.
	 * @param value the value
	 * @return the resulting timestamp or null
	 */
	private @Nullable Instant timestampOrNull(@Nullable String value) {
		return (value == null || value.isBlank()) ? null : timestamp(value);
	}

	/**
	 * Maps a provider qualifier to an observation quality.
	 * @param qualifier the qualifier
	 * @return the resulting quality
	 */
	private ObservationQuality quality(@Nullable String qualifier) {
		if (qualifier == null || qualifier.isBlank() || qualifier.equalsIgnoreCase("Z")) {
			return ObservationQuality.VALID;
		}
		if (qualifier.matches("[A-Za-z]")) {
			return ObservationQuality.SUSPECT;
		}
		return ObservationQuality.MALFORMED_QUALIFIER;
	}

	/**
	 * Parses CSV line.
	 * @param line the line
	 * @return the parsed result
	 */
	private List<String> parseCsvLine(String line) {
		List<String> values = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean quoted = false;
		for (int index = 0; index < line.length(); index++) {
			char character = line.charAt(index);
			if (character == '"') {
				if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
					current.append('"');
					index++;
				}
				else {
					quoted = !quoted;
				}
			}
			else if (character == ',' && !quoted) {
				values.add(current.toString());
				current.setLength(0);
			}
			else {
				current.append(character);
			}
		}
		if (quoted) {
			throw new IllegalArgumentException("unterminated quoted CSV field");
		}
		values.add(current.toString());
		return values;
	}

	/**
	 * Describes a variable column.
	 * @param index the zero-based value index
	 * @param sourceKey the source key
	 * @param normalizedKey the normalized key
	 * @author Gunnar Hillert
	 */
	private record VariableColumn(int index, String sourceKey, String normalizedKey) {
	}

}
