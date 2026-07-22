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

package com.pembana.raingauge.rainfall;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Describes a rainfall amount.
 * @param inches the inches
 * @author Gunnar Hillert
 */
public record RainfallAmount(BigDecimal inches) {

	private static final BigDecimal MILLIMETERS_PER_INCH = new BigDecimal("25.4");

	/**
	 * Creates a new {@code RainfallAmount}.
	 * @param inches the inches
	 */
	public RainfallAmount {
		inches = inches.stripTrailingZeros();
	}

	/**
	 * Converts this rainfall amount to millimeters.
	 * @return the resulting millimeters
	 */
	public BigDecimal millimeters() {
		return this.inches.multiply(MILLIMETERS_PER_INCH);
	}

	/**
	 * Returns this rainfall amount in the requested unit.
	 * @param unit the requested rainfall unit
	 * @return the resulting value
	 */
	public BigDecimal value(RainfallUnit unit) {
		return unit == RainfallUnit.IMPERIAL ? this.inches : millimeters();
	}

	/**
	 * Returns the display precision for a rainfall unit.
	 * @param unit the requested rainfall unit
	 * @return the resulting display scale
	 */
	public int displayScale(RainfallUnit unit) {
		return unit == RainfallUnit.IMPERIAL ? 2 : 1;
	}

	/**
	 * Formats the rainfall amount for display.
	 * @param unit the requested rainfall unit
	 * @return the formatted display value
	 */
	public String display(RainfallUnit unit) {
		return value(unit).setScale(displayScale(unit), RoundingMode.HALF_UP).toPlainString()
				+ ' ' + unit.symbol();
	}

}
