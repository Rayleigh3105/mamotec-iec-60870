package org.mamotec.responder.modbus;

import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import org.mamotec.common.enums.NeTable;
import org.mamotec.common.type.ValueHolder;

import java.net.InetAddress;

public class ModbusTcpClient {
	public static final int UNIT = 32;

	private final TCPMasterConnection connection;

	private ModbusTCPTransaction transaction;

	private final ModbusReadService readService;

	private final ModbusWriteService writeService;

	public ModbusTcpClient(String modbusTcpIp) throws Exception {
		InetAddress address = InetAddress.getByName(modbusTcpIp);
		connection = new TCPMasterConnection(address);
		connection.setPort(502); // Standard-Modbus-TCP-Port
		connection.connect();
		readService = new ModbusReadService(connection, transaction, UNIT);
		writeService = new ModbusWriteService(connection, transaction, UNIT);
	}

	public <T> ValueHolder<T> read(NeTable neTable) {
		return readService.read(neTable);
	}

	public <T> boolean write(NeTable neTable, ValueHolder<T> valueHolder) {
		return writeService.write(neTable, valueHolder);
	}

	public void close() {
		if (connection != null && connection.isConnected()) {
			connection.close();
		}
	}
}
