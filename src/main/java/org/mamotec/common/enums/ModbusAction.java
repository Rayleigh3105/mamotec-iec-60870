package org.mamotec.common.enums;

public enum ModbusAction {

	// Boolean value
	READ_COIL(Boolean.class),
	WRITE_COIL(Boolean.class),

	READ_HOLDING_REGISTER(Integer.class),
	READ_HOLDING_REGISTER_FLOAT(Float.class),
	WRITE_HOLDING_REGISTER(Integer.class),
	WRITE_HOLDING_REGISTER_FLOAT(Float.class);

	private final Class<?> type;

	ModbusAction(Class<?> returnType) {
		this.type = returnType;
	}

	public Class<?> getReturnType() {
		return type;
	}
}
