package org.mamotec.common.enums;

import lombok.Getter;
import lombok.Setter;
import org.mamotec.common.threshhold.AbsoluteThreshold;
import org.mamotec.common.threshhold.AdditiveThreshold;
import org.mamotec.common.threshhold.Threshold;
import org.mamotec.j60870.ASduType;

import java.util.ArrayList;
import java.util.List;

@Getter
public enum NeTable {

	// @formatter:off
	DUMMY(0, Unit.NONE, AccessType.READ, ASduType.M_ME_TF_1, ModbusAction.READ_HOLDING_REGISTER_FLOAT, List.of(new AbsoluteThreshold(0.1), new AdditiveThreshold(150))),
	SWITCH_POSITION(1, Unit.NONE, AccessType.READ, ASduType.M_DP_TB_1, ModbusAction.READ_COIL),
	EQUIPMENT_MALFUNCTION(10, Unit.NONE, AccessType.READ, ASduType.M_SP_TB_1, ModbusAction.READ_COIL),
	SWITCH_CASE(11, Unit.NONE, AccessType.READ, ASduType.M_SP_TB_1, ModbusAction.READ_COIL),
	KSA_1(12, Unit.NONE, AccessType.READ, ASduType.M_SP_TB_1, ModbusAction.READ_COIL),
	KSA_UE(15, Unit.NONE, AccessType.READ, ASduType.M_DP_TB_1, ModbusAction.READ_COIL),
	E_UE_WIPER(16, Unit.NONE, AccessType.READ, ASduType.M_SP_TB_1, ModbusAction.READ_COIL),
	E_UE_DEPTH_LOCATION(17, Unit.NONE, AccessType.READ, ASduType.M_SP_TB_1, ModbusAction.READ_COIL),
	E_GRID_WIPER(18, Unit.NONE, AccessType.READ, ASduType.M_SP_TB_1, ModbusAction.READ_COIL),
	E_GRID_DEPTH_LOCATION(19, Unit.NONE, AccessType.READ, ASduType.M_SP_TB_1, ModbusAction.READ_COIL),

	// Vorgabe Einspeise-/Blindleistungsmanagement
	SPEC_ACTIVE_POWER_EZA1(111, Unit.PERCENT, AccessType.WRITE, ASduType.C_SE_NC_1, ModbusAction.WRITE_HOLDING_REGISTER_FLOAT),
	SPEC_ACTIVE_POWER_EZA2(112, Unit.PERCENT, AccessType.WRITE, ASduType.C_SE_NC_1, ModbusAction.WRITE_HOLDING_REGISTER_FLOAT),
	SPEC_ACTIVE_POWER_EZA3(113, Unit.PERCENT, AccessType.WRITE, ASduType.C_SE_NC_1, ModbusAction.WRITE_HOLDING_REGISTER_FLOAT),
	SPEC_ACTIVE_POWER_EZA4(114, Unit.PERCENT, AccessType.WRITE, ASduType.C_SE_NC_1, ModbusAction.WRITE_HOLDING_REGISTER_FLOAT),
	SPEC_POWER_FACTOR(115, Unit.NONE, AccessType.WRITE, ASduType.C_SE_NC_1, ModbusAction.WRITE_HOLDING_REGISTER_FLOAT),

	// Measurements
	VOLTAGE_L1_L2_NVP(5, Unit.VOLT, AccessType.READ, ASduType.M_ME_TF_1, ModbusAction.READ_HOLDING_REGISTER),
	CURRENT_L2_NVP(42, Unit.AMPERE, AccessType.READ, ASduType.M_ME_TF_1, ModbusAction.READ_HOLDING_REGISTER_FLOAT),
	ACTIVE_POWER_NVP(43, Unit.WATT, AccessType.READ, ASduType.M_ME_TF_1, ModbusAction.READ_HOLDING_REGISTER_FLOAT),
	REACTIVE_POWER_NVP(44, Unit.WATT, AccessType.READ, ASduType.M_ME_TF_1, ModbusAction.READ_HOLDING_REGISTER_FLOAT),

	ACTUAL_ACTIVE_POWER_FEED_IN_EZA1(151, Unit.WATT, AccessType.READ, ASduType.M_ME_TF_1, ModbusAction.READ_HOLDING_REGISTER_FLOAT),
	ACTUAL_ACTIVE_POWER_FEED_IN_EZA2(152, Unit.WATT, AccessType.READ, ASduType.M_ME_TF_1, ModbusAction.READ_HOLDING_REGISTER_FLOAT),
	ACTUAL_ACTIVE_POWER_FEED_IN_EZA3(153, Unit.WATT, AccessType.READ, ASduType.M_ME_TF_1, ModbusAction.READ_HOLDING_REGISTER_FLOAT),
	ACTUAL_ACTIVE_POWER_FEED_IN_EZA4(154, Unit.WATT, AccessType.READ, ASduType.M_ME_TF_1, ModbusAction.READ_HOLDING_REGISTER_FLOAT),

	ACTUAL_REACTIVE_POWER_FEED_IN_EZA1(155, Unit.WATT, AccessType.READ, ASduType.M_ME_TF_1, ModbusAction.READ_HOLDING_REGISTER_FLOAT),
	ACTUAL_REACTIVE_POWER_FEED_IN_EZA2(156, Unit.WATT, AccessType.READ, ASduType.M_ME_TF_1, ModbusAction.READ_HOLDING_REGISTER_FLOAT),
	ACTUAL_REACTIVE_POWER_FEED_IN_EZA3(157, Unit.WATT, AccessType.READ, ASduType.M_ME_TF_1, ModbusAction.READ_HOLDING_REGISTER_FLOAT),
	ACTUAL_REACTIVE_POWER_FEED_IN_EZA4(158, Unit.WATT, AccessType.READ, ASduType.M_ME_TF_1, ModbusAction.READ_HOLDING_REGISTER_FLOAT),

	ACTIVE_POWER_POTENTIAL_EZA1(161, Unit.WATT, AccessType.READ, ASduType.M_ME_TF_1, ModbusAction.READ_HOLDING_REGISTER_FLOAT),
	ACTIVE_POWER_POTENTIAL_EZA2(162, Unit.WATT, AccessType.READ, ASduType.M_ME_TF_1, ModbusAction.READ_HOLDING_REGISTER_FLOAT),
	ACTIVE_POWER_POTENTIAL_EZA3(163, Unit.WATT, AccessType.READ, ASduType.M_ME_TF_1, ModbusAction.READ_HOLDING_REGISTER_FLOAT),
	ACTIVE_POWER_POTENTIAL_EZA4(164, Unit.WATT, AccessType.READ, ASduType.M_ME_TF_1, ModbusAction.READ_HOLDING_REGISTER_FLOAT),

	// Rückmeldung Vorgabe Einspeise-/Blindleistungsmanagement
	RETURN_ACTIVE_POWER_EZA1(211, Unit.PERCENT, AccessType.READ, ASduType.C_SE_NC_1, ModbusAction.READ_HOLDING_REGISTER_FLOAT),
	RETURN_ACTIVE_POWER_EZA2(212, Unit.PERCENT, AccessType.READ, ASduType.C_SE_NC_1, ModbusAction.READ_HOLDING_REGISTER_FLOAT),
	RETURN_ACTIVE_POWER_EZA3(213, Unit.PERCENT, AccessType.READ, ASduType.C_SE_NC_1, ModbusAction.READ_HOLDING_REGISTER_FLOAT),
	RETURN_ACTIVE_POWER_EZA4(214, Unit.PERCENT, AccessType.READ, ASduType.C_SE_NC_1, ModbusAction.READ_HOLDING_REGISTER_FLOAT),
	RETURN_SPEC_POWER_FACTOR(215, Unit.NONE, AccessType.READ, ASduType.C_SE_NC_1, ModbusAction.READ_HOLDING_REGISTER_FLOAT);


	// @formatter:on

	private final int register;

	private final Unit unit;

	private final AccessType modbusAccessType;

	private final ASduType asduType;

	private final ModbusAction modbusAction;

	private final List<Threshold> thresholds;

	@Setter
	private Object lastMeasuredValue;

	NeTable(int register, Unit unit, AccessType modbusAccessType, ASduType asduType, ModbusAction modbusAction, List<Threshold> thresholds) {
		this.register = register;
		this.unit = unit;
		this.modbusAccessType = modbusAccessType;
		this.asduType = asduType;
		this.modbusAction = modbusAction;
		this.thresholds = thresholds;
	}

	NeTable(int register, Unit unit, AccessType modbusAccessType, ASduType asduType, ModbusAction modbusAction) {
		this.register = register;
		this.unit = unit;
		this.modbusAccessType = modbusAccessType;
		this.asduType = asduType;
		this.modbusAction = modbusAction;
		this.thresholds = new ArrayList<>();
	}

}
