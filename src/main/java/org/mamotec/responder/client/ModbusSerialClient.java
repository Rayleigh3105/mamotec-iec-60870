package org.mamotec.responder.client;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusSerialTransaction;
import com.ghgande.j2mod.modbus.msg.ReadCoilsRequest;
import com.ghgande.j2mod.modbus.msg.ReadCoilsResponse;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersResponse;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterRequest;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterResponse;
import com.ghgande.j2mod.modbus.net.SerialConnection;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import com.ghgande.j2mod.modbus.util.SerialParameters;

import java.nio.ByteBuffer;

public class ModbusSerialClient {
	private final SerialConnection connection;

	private ModbusSerialTransaction transaction;

	public ModbusSerialClient(String portName) {
		SerialParameters params = new SerialParameters();
		params.setPortName(portName);
		params.setBaudRate(9600);
		params.setDatabits(8);
		params.setParity(0);
		params.setStopbits(0);
		params.setEncoding(Modbus.SERIAL_ENCODING_RTU);
		params.setEcho(false);

		connection = new SerialConnection(params);
		try {
			connection.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean[] readCoils(int address, int count, int unit) {
		try {
			ReadCoilsRequest request = new ReadCoilsRequest(address, count);
			request.setUnitID(unit);
			transaction = new ModbusSerialTransaction(connection);
			transaction.setRequest(request);
			transaction.execute();

			ReadCoilsResponse response = (ReadCoilsResponse) transaction.getResponse();
			boolean[] coils = new boolean[count];
			for (int i = 0; i < count; i++) {
				coils[i] = response.getCoilStatus(i);
			}
			return coils;
		} catch (ModbusException e) {
			e.printStackTrace();
			return null;
		}
	}

	public boolean writeRegister(int address, int value, int unit) {
		try {
			WriteSingleRegisterRequest request = new WriteSingleRegisterRequest(address, new SimpleRegister(value));
			request.setUnitID(unit);
			transaction = new ModbusSerialTransaction(connection);
			transaction.setRequest(request);
			transaction.execute();

			WriteSingleRegisterResponse response = (WriteSingleRegisterResponse) transaction.getResponse();
			return response != null;
		} catch (ModbusException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean write32BitRegister(int address, float value, int unit) {
		try {
			int[] uint16Array = convertFloatToUInt16Array(value);

			WriteSingleRegisterRequest requestHigh = new WriteSingleRegisterRequest(address, new SimpleRegister(uint16Array[0]));
			requestHigh.setUnitID(unit);
			transaction = new ModbusSerialTransaction(connection);
			transaction.setRequest(requestHigh);
			transaction.execute();

			WriteSingleRegisterRequest requestLow = new WriteSingleRegisterRequest(address + 1, new SimpleRegister(uint16Array[1]));
			requestLow.setUnitID(unit);
			transaction = new ModbusSerialTransaction(connection);
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

	public float read32BitRegister(int address, int unit) {
		try {
			ReadInputRegistersRequest requestHigh = new ReadInputRegistersRequest(address, 1);
			requestHigh.setUnitID(unit);
			transaction = new ModbusSerialTransaction(connection);
			transaction.setRequest(requestHigh);
			transaction.execute();

			ReadInputRegistersResponse responseHigh = (ReadInputRegistersResponse) transaction.getResponse();
			int highRegister = responseHigh.getRegisterValue(0);

			ReadInputRegistersRequest requestLow = new ReadInputRegistersRequest(address + 1, 1);
			requestLow.setUnitID(unit);
			transaction = new ModbusSerialTransaction(connection);
			transaction.setRequest(requestLow);
			transaction.execute();

			ReadInputRegistersResponse responseLow = (ReadInputRegistersResponse) transaction.getResponse();
			int lowRegister = responseLow.getRegisterValue(0);

			return convertUInt16ArrayToFloat(new int[] { highRegister, lowRegister });
		} catch (ModbusException e) {
			e.printStackTrace();
			return Float.NaN;
		}
	}

	private float convertUInt16ArrayToFloat(int[] uint16Array) {
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.putShort((short) uint16Array[0]);
		buffer.putShort((short) uint16Array[1]);
		buffer.rewind();
		return buffer.getFloat();
	}

	public void close() {
		if (connection != null && connection.isOpen()) {
			connection.close();
		}
	}
}
