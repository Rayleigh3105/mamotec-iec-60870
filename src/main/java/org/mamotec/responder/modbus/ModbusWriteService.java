package org.mamotec.responder.modbus;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.WriteCoilRequest;
import com.ghgande.j2mod.modbus.msg.WriteCoilResponse;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterRequest;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterResponse;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import org.mamotec.common.enums.NeTable;
import org.mamotec.common.type.ValueHolder;

import java.nio.ByteBuffer;

public class ModbusWriteService {

	private final TCPMasterConnection connection;

	private ModbusTCPTransaction transaction;

	private final int unit;

	public ModbusWriteService(TCPMasterConnection connection, ModbusTCPTransaction transaction, int unit) {
		this.connection = connection;
		this.transaction = transaction;
		this.unit = unit;
	}

	public <T> boolean write(NeTable neTable, ValueHolder<T> valueHolder) {
		return switch (neTable.getModbusAction()) {
			case WRITE_COIL -> writeCoil(neTable.getRegister(), (Boolean) valueHolder.getValue());
			case WRITE_HOLDING_REGISTER -> writeRegister(neTable.getRegister(), (Integer) valueHolder.getValue());
			case WRITE_HOLDING_REGISTER_FLOAT -> write32BitRegister(neTable.getRegister(), (Float) valueHolder.getValue());
			default -> throw new IllegalArgumentException("Unsupported ModbusAction: " + neTable.getModbusAction());
		};

	}

	public boolean writeCoil(int address, boolean value) {
		try {
			WriteCoilRequest request = new WriteCoilRequest(address, value);
			request.setUnitID(unit);
			transaction = new ModbusTCPTransaction(connection);
			transaction.setRequest(request);
			transaction.execute();

			WriteCoilResponse response = (WriteCoilResponse) transaction.getResponse();
			return response != null;
		} catch (ModbusException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean writeRegister(int address, int value) {
		try {
			WriteSingleRegisterRequest request = new WriteSingleRegisterRequest(address, new SimpleRegister(value));
			request.setUnitID(unit);
			transaction = new ModbusTCPTransaction(connection);
			transaction.setRequest(request);
			transaction.execute();

			WriteSingleRegisterResponse response = (WriteSingleRegisterResponse) transaction.getResponse();
			return response != null;
		} catch (ModbusException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean write32BitRegister(int address, float value) {
		try {
			int[] uint16Array = convertFloatToUInt16Array(value);

			WriteSingleRegisterRequest requestHigh = new WriteSingleRegisterRequest(address, new SimpleRegister(uint16Array[0]));
			requestHigh.setUnitID(unit);
			transaction = new ModbusTCPTransaction(connection);
			transaction.setRequest(requestHigh);
			transaction.execute();

			WriteSingleRegisterRequest requestLow = new WriteSingleRegisterRequest(address + 1, new SimpleRegister(uint16Array[1]));
			requestLow.setUnitID(unit);
			transaction = new ModbusTCPTransaction(connection);
			transaction.setRequest(requestLow);
			transaction.execute();

			WriteSingleRegisterResponse responseHigh = (WriteSingleRegisterResponse) transaction.getResponse();
			WriteSingleRegisterResponse responseLow = (WriteSingleRegisterResponse) transaction.getResponse();

			return responseHigh != null && responseLow != null;
		} catch (ModbusException e) {
			e.printStackTrace();
			return false;
		}
	}

	private int[] convertFloatToUInt16Array(float floatValue) {
		int[] result = new int[2];
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.putFloat(floatValue);
		buffer.rewind();
		result[0] = buffer.getShort() & 0xFFFF;
		result[1] = buffer.getShort() & 0xFFFF;
		return result;
	}
}
