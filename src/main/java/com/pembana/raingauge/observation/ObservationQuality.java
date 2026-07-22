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

/**
 * Enumerates the supported observation quality values.
 * @author Gunnar Hillert
 */
public enum ObservationQuality {

	/** Observation passed all provider quality checks. */
	VALID,

	/** Provider marked the observation as suspect. */
	SUSPECT,

	/** Observation value is absent. */
	MISSING,

	/** Provider supplied a qualifier that could not be interpreted. */
	MALFORMED_QUALIFIER

}
