package com.pembana.raingauge.rainfall;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record RainfallAmount(BigDecimal inches) {

	private static final BigDecimal MILLIMETERS_PER_INCH = new BigDecimal("25.4");

	public RainfallAmount {
		inches = inches.stripTrailingZeros();
	}

	public BigDecimal millimeters() {
		return this.inches.multiply(MILLIMETERS_PER_INCH);
	}

	public BigDecimal value(RainfallUnit unit) {
		return unit == RainfallUnit.IMPERIAL ? this.inches : millimeters();
	}

	public int displayScale(RainfallUnit unit) {
		return unit == RainfallUnit.IMPERIAL ? 2 : 1;
	}

	public String display(RainfallUnit unit) {
		return value(unit).setScale(displayScale(unit), RoundingMode.HALF_UP).toPlainString()
				+ ' ' + unit.symbol();
	}

}
