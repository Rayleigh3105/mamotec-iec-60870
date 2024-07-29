package org.mamotec.common.type;

public class ValueHolder<T> {

	private final Class<T> type;

	private T value;

	public ValueHolder(Class<T> type, T value) {
		this.type = type;
		this.value = value;
	}

	public Class<T> getType() {
		return type;
	}

	public T getValue() {
		return value;
	}

	public void setValue(T value) {
		this.value = value;
	}

}
