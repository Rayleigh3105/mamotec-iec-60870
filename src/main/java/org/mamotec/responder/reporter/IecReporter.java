package org.mamotec.responder.reporter;

import org.mamotec.common.enums.AccessType;
import org.mamotec.common.enums.NeTable;
import org.mamotec.common.type.ValueHolder;
import org.mamotec.responder.client.IecClient;
import org.mamotec.responder.modbus.ModbusTcpClient;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.util.Arrays.stream;
import static org.mamotec.common.enums.NeTable.values;

public class IecReporter {

	public static final String MODBUS_TCP_IP = "127.0.0.1";

	private final List<NeTable> readRegister = new ArrayList<>();

	private final IecClient iecGateway;

	public  IecReporter(IecClient iecGateway) {
		this.iecGateway = iecGateway;
		readRegister.addAll(stream(values()).filter(neTable -> neTable.getModbusAccessType().equals(AccessType.READ)).toList());
	}

	public void doReport() {
		ModbusTcpClient tcpClient;

		try {
			tcpClient = new ModbusTcpClient(MODBUS_TCP_IP);

			ModbusTcpClient modbusServer = tcpClient;
			readRegister.forEach(neTable -> {
				// Read table from Modbus Server
				ValueHolder<Object> holder = modbusServer.read(neTable);

				// Send command via IEC Client
				boolean isSent = false;
				try {
					isSent = iecGateway.sendValue(holder, neTable);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}

				if (isSent) {
					log("Value sent successfully: " + holder.getValue());
				} else {
					log("Failed to send value: " + holder.getValue());
				}
			});

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	private void log(String message) {
		String time = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS ").format(new Date());
		System.out.println(time + message);
	}

}
