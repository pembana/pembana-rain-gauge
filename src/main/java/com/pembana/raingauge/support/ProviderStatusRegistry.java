package com.pembana.raingauge.support;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
public class ProviderStatusRegistry {

	private final AtomicReference<ProviderState> catalog = new AtomicReference<>(ProviderState.unknown());

	private final AtomicReference<ProviderState> observations = new AtomicReference<>(ProviderState.unknown());

	public void catalogSucceeded(Instant at, long durationMillis) {
		this.catalog.set(ProviderState.available(at, durationMillis));
	}

	public void catalogFailed(Instant at, String message) {
		this.catalog.set(ProviderState.failed(at, message, this.catalog.get().lastSuccess()));
	}

	public void observationsSucceeded(Instant at, long durationMillis) {
		this.observations.set(ProviderState.available(at, durationMillis));
	}

	public void observationsFailed(Instant at, String message) {
		this.observations.set(ProviderState.failed(at, message, this.observations.get().lastSuccess()));
	}

	public ProviderState catalog() {
		return this.catalog.get();
	}

	public ProviderState observations() {
		return this.observations.get();
	}

	public record ProviderState(
			boolean known,
			boolean available,
			@Nullable Instant lastAttempt,
			@Nullable Instant lastSuccess,
			@Nullable Long latencyMillis,
			@Nullable String error) {

		static ProviderState unknown() {
			return new ProviderState(false, false, null, null, null, null);
		}

		static ProviderState available(Instant at, long durationMillis) {
			return new ProviderState(true, true, at, at, durationMillis, null);
		}

		static ProviderState failed(Instant at, String message, @Nullable Instant lastSuccess) {
			return new ProviderState(true, false, at, lastSuccess, null, message);
		}
	}

}
