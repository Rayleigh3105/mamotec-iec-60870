package org.mamotec.common.enums;

public enum ModbusAction {

	// Boolean value
	READ_COIL,
	WRITE_COIL,

	// Single 16-bit value
	READ_HOLDING_REGISTER,
	WRITE_HOLDING_REGISTER,

	// Multiple 32-bit values
	READ_INPUT_REGISTER,
	WRITE_INPUT_REGISTER


}
