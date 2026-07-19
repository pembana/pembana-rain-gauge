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

@Component
public class HadsObservationParser {

	private static final DateTimeFormatter IEM_TIMESTAMP =
			DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");

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
			} catch (RuntimeException ex) {
				rejectedRows++;
				warnings.add("Row " + (lineIndex + 1) + " rejected: " + ex.getMessage());
			}
		}
		return new ObservationParseResult(observations, warnings, parsedRows, rejectedRows);
	}

	private Map<String, Integer> indexColumns(List<String> header) {
		Map<String, Integer> columns = new HashMap<>();
		for (int index = 0; index < header.size(); index++) {
			columns.put(header.get(index).strip().toLowerCase(Locale.ROOT), index);
		}
		return columns;
	}

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

	private String normalizeShefKey(String key) {
		String normalized = key.strip().toUpperCase(Locale.ROOT);
		return normalized.endsWith("ZZ") ? normalized.substring(0, normalized.length() - 2)
				: normalized;
	}

	private @Nullable Integer first(Map<String, Integer> columns, String... names) {
		for (String name : names) {
			Integer column = columns.get(name);
			if (column != null) {
				return column;
			}
		}
		return null;
	}

	private String required(List<String> values, int index, String field) {
		String value = value(values, index);
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " was missing");
		}
		return value;
	}

	private @Nullable String value(List<String> values, @Nullable Integer index) {
		return index != null && index >= 0 && index < values.size() ? values.get(index).strip() : null;
	}

	private Instant timestamp(String value) {
		try {
			return Instant.parse(value);
		} catch (DateTimeParseException ex) {
			try {
				return LocalDateTime.parse(value, IEM_TIMESTAMP).toInstant(ZoneOffset.UTC);
			} catch (DateTimeParseException nested) {
				throw new IllegalArgumentException("timestamp was invalid", nested);
			}
		}
	}

	private @Nullable Instant timestampOrNull(@Nullable String value) {
		return value == null || value.isBlank() ? null : timestamp(value);
	}

	private ObservationQuality quality(@Nullable String qualifier) {
		if (qualifier == null || qualifier.isBlank() || qualifier.equalsIgnoreCase("Z")) {
			return ObservationQuality.VALID;
		}
		if (qualifier.matches("[A-Za-z]")) {
			return ObservationQuality.SUSPECT;
		}
		return ObservationQuality.MALFORMED_QUALIFIER;
	}

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
				} else {
					quoted = !quoted;
				}
			} else if (character == ',' && !quoted) {
				values.add(current.toString());
				current.setLength(0);
			} else {
				current.append(character);
			}
		}
		if (quoted) {
			throw new IllegalArgumentException("unterminated quoted CSV field");
		}
		values.add(current.toString());
		return values;
	}

	private record VariableColumn(int index, String sourceKey, String normalizedKey) {
	}

}
