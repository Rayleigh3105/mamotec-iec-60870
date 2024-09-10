package org.mamotec.thread;

import lombok.extern.slf4j.Slf4j;
import org.mamotec.j60870.serial.SerialConnection;
import org.mamotec.responder.client.IecClient;
import org.mamotec.responder.modbus.ModbusTcpClient;
import org.mamotec.responder.reporter.IecReporter;

@Slf4j
public class IecThread extends Thread {

	private final SerialConnection serialConnection;

	private final ModbusTcpClient modbusTcpClient;

	public IecThread(SerialConnection serialConnection, ModbusTcpClient modbusTcpClient) {
		this.serialConnection = serialConnection;
		this.modbusTcpClient = modbusTcpClient;
	}

	@Override
	public void run() {
		Thread.currentThread().setName("IecThread");

		try {
			IecClient iecClient = new IecClient(serialConnection);
			IecReporter iecReporter = new IecReporter(iecClient, modbusTcpClient, 100);
			iecReporter.startReporting();
		} catch (Exception e) {
			log.error("Error opening serial connection: {}", e.getMessage());
		}
	}
}
