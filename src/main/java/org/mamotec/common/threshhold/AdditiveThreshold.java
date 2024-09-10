package org.mamotec.common.threshhold;

public class AdditiveThreshold implements Threshold{

	private final double thresholdPercentage;

	public AdditiveThreshold(double thresholdPercentage) {
		this.thresholdPercentage = thresholdPercentage;
	}

	@Override
	public boolean check(Number currentValue, Number lastValue) {
		double thresholdValue = lastValue.doubleValue() * (1 + thresholdPercentage / 100);
		return currentValue.doubleValue() > thresholdValue;	}
}
