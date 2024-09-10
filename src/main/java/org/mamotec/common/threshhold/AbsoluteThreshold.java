package org.mamotec.common.threshhold;

public class AbsoluteThreshold implements Threshold {

	private final double thresholdPercentage;

	public AbsoluteThreshold(double thresholdPercentage) {
		this.thresholdPercentage = thresholdPercentage;
	}

	@Override
	public boolean check(Number currentValue, Number lastValue) {
		return Math.abs(currentValue.doubleValue() - lastValue.doubleValue()) > thresholdPercentage;
	}
}
