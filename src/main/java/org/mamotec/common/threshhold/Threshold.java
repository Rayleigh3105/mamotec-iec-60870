package org.mamotec.common.threshhold;

public interface Threshold {

	boolean check(Number currentValue, Number lastValue);
}
