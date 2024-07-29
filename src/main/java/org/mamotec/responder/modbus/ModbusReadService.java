package org.mamotec.responder.modbus;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.ReadCoilsRequest;
import com.ghgande.j2mod.modbus.msg.ReadCoilsResponse;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersResponse;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import org.mamotec.common.enums.NeTable;
import org.mamotec.common.type.ValueHolder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ModbusReadService {

	private ModbusTCPTransaction transaction;

	private final TCPMasterConnection connection;

	private final int unit;

	public ModbusReadService(TCPMasterConnection connection, ModbusTCPTransaction transaction, int unit) {
		this.connection = connection;
		this.transaction = transaction;
		this.unit = unit;
	}

	public <T> ValueHolder<T> read(NeTable neTable) {
		try {
			return switch (neTable.getModbusAction()) {
				case READ_COIL -> new ValueHolder(Boolean.class, readCoil(neTable.getRegister()));
				case READ_HOLDING_REGISTER -> new ValueHolder(Float.class, readHoldingRegister(neTable.getRegister()));
				case READ_HOLDING_REGISTER_FLOAT -> new ValueHolder(Float.class, readFloatFromHoldingRegisters(neTable.getRegister()));
				default -> throw new IllegalArgumentException("Unsupported ModbusAction: " + neTable.getModbusAction());
			};
		} catch (ModbusException e) {
			throw new RuntimeException(e);
		}
	}

	/////////////////////////////////////////////
	// COIL
	/////////////////////////////////////////////

	public Boolean readCoil(int address) {
		try {
			ReadCoilsRequest request = new ReadCoilsRequest(address, 1);
			request.setUnitID(unit);
			transaction = new ModbusTCPTransaction(connection);
			transaction.setRequest(request);
			transaction.execute();

			ReadCoilsResponse response = (ReadCoilsResponse) transaction.getResponse();
			boolean[] coils = new boolean[1];
			for (int i = 0; i < 1; i++) {
				coils[i] = response.getCoilStatus(i);
			}
			return coils[0];
		} catch (ModbusException e) {
			e.printStackTrace();
			return null;
		}
	}

	/////////////////////////////////////////////
	// INPUT REGISTER
	/////////////////////////////////////////////

	public Integer[] readInputRegisters(int address, int count) {
		try {
			ReadInputRegistersRequest request = new ReadInputRegistersRequest(address, count);
			transaction = new ModbusTCPTransaction(connection);
			transaction.setRequest(request);
			transaction.execute();
			ReadInputRegistersResponse response = (ReadInputRegistersResponse) transaction.getResponse();
			Integer[] registers = new Integer[count];
			for (int i = 0; i < count; i++) {
				registers[i] = response.getRegisterValue(i);
			}
			return registers;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Integer readInputRegister(int address) throws ModbusException {
		return readInputRegisters(address, 1)[0];
	}

	/////////////////////////////////////////////
	// HOLDING REGISTER
	/////////////////////////////////////////////

	public int[] readHoldingRegisters(int ref, int count) throws ModbusException {
		ReadMultipleRegistersRequest request = new ReadMultipleRegistersRequest(ref, count);
		transaction = new ModbusTCPTransaction(connection);
		transaction.setRequest(request);
		transaction.execute();
		ReadMultipleRegistersResponse response = (ReadMultipleRegistersResponse) transaction.getResponse();
		int[] registers = new int[count];
		for (int i = 0; i < count; i++) {
			registers[i] = response.getRegisterValue(i);
		}
		return registers;
	}

	public Float readFloatFromHoldingRegisters(int address) throws ModbusException {
		int[] registers = readHoldingRegisters(address, 2);

		int high = registers[0];
		int low = registers[1];
		int combined = (high << 16) | (low & 0xFFFF);

		return ByteBuffer.wrap(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(combined).array()).getFloat();
	}

	public Integer readHoldingRegister(int address) throws ModbusException {
		int[] registers = readHoldingRegisters(address, 1);

		return registers[0];
	}

}
