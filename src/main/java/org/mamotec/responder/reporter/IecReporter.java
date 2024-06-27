package org.mamotec.responder.reporter;

import org.mamotec.common.enums.AccessType;
import org.mamotec.common.enums.NeModbusTable;
import org.mamotec.responder.client.IecClient;
import org.mamotec.responder.client.ModbusTcpClient;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IecReporter {

	private List<NeModbusTable> readRegister = new ArrayList<>();

	private final IecClient client;

	public IecReporter(IecClient client) {
		this.client = client;
		readRegister.addAll(Arrays.stream(NeModbusTable.values()).filter(neModbusTable -> neModbusTable.getModbusAccessType().equals(AccessType.READ)).toList());
	}

	public void doReport() {
		ModbusTcpClient tcpClient = null;
		try {
			tcpClient = new ModbusTcpClient("127.0.0.1");

			ModbusTcpClient finalTcpClient = tcpClient;
			readRegister.forEach(neModbusTable -> {
				neModbusTable.getModbusAction();
				// Read table from Modbus Server
				Object testing = finalTcpClient.read(neModbusTable.getRegister(), 15);
				System.out.println(testing);

				// Send command via IEC Client
			});
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}

	}

}
