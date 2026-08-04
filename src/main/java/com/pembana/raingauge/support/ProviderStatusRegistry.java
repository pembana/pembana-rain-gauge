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

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Tracks provider status.
 * @author Gunnar Hillert
 */
@Component
public class ProviderStatusRegistry {

	private final AtomicReference<ProviderState> catalog = new AtomicReference<>(ProviderState.unknown());

	private final AtomicReference<ProviderState> observations = new AtomicReference<>(ProviderState.unknown());

	/**
	 * Records a successful station-catalog request.
	 * @param at the time at which the provider operation completed
	 * @param durationMillis the provider operation duration in milliseconds
	 */
	public void catalogSucceeded(Instant at, long durationMillis) {
		this.catalog.set(ProviderState.available(at, durationMillis));
	}

	/**
	 * Records a failed station-catalog request.
	 * @param at the time at which the provider operation completed
	 * @param message the detail message
	 */
	public void catalogFailed(Instant at, String message) {
		this.catalog.set(ProviderState.failed(at, message, this.catalog.get().lastSuccess()));
	}

	/**
	 * Records a successful observation request.
	 * @param at the time at which the provider operation completed
	 * @param durationMillis the provider operation duration in milliseconds
	 */
	public void observationsSucceeded(Instant at, long durationMillis) {
		this.observations.set(ProviderState.available(at, durationMillis));
	}

	/**
	 * Records a failed observation request.
	 * @param at the time at which the provider operation completed
	 * @param message the detail message
	 */
	public void observationsFailed(Instant at, String message) {
		this.observations.set(ProviderState.failed(at, message, this.observations.get().lastSuccess()));
	}

	/**
	 * Returns the station-catalog provider state.
	 * @return the resulting catalog
	 */
	public ProviderState catalog() {
		return this.catalog.get();
	}

	/**
	 * Returns rainfall increments for a station.
	 * @return the resulting observations
	 */
	public ProviderState observations() {
		return this.observations.get();
	}

	/**
	 * Describes a provider state.
	 * @param known the known
	 * @param available the available
	 * @param lastAttempt the last attempt
	 * @param lastSuccess the last success
	 * @param latencyMillis the latency millis
	 * @param error the error
	 * @author Gunnar Hillert
	 */
	public record ProviderState(
			boolean known,
			boolean available,
			@Nullable Instant lastAttempt,
			@Nullable Instant lastSuccess,
			@Nullable Long latencyMillis,
			@Nullable String error) {

		/**
		 * Creates an unknown provider state.
		 * @return the resulting unknown
		 */
		static ProviderState unknown() {
			return new ProviderState(false, false, null, null, null, null);
		}

		/**
		 * Creates an available provider state.
		 * @param at the time at which the provider operation completed
		 * @param durationMillis the provider operation duration in milliseconds
		 * @return the resulting available
		 */
		static ProviderState available(Instant at, long durationMillis) {
			return new ProviderState(true, true, at, at, durationMillis, null);
		}

		/**
		 * Creates a failed provider state while retaining prior success metadata.
		 * @param at the time at which the provider operation completed
		 * @param message the detail message
		 * @param lastSuccess the last success
		 * @return the resulting failed
		 */
		static ProviderState failed(Instant at, String message, @Nullable Instant lastSuccess) {
			return new ProviderState(true, false, at, lastSuccess, null, message);
		}
	}

}
