package org.mamotec.thread;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.mamotec.j60870.serial.SerialConnection;
import org.mamotec.j60870.serial.SerialPortDataListenerImpl;

@Slf4j
@Getter
public class SerialConnectionReadThread extends Thread {

	private final SerialConnection serialConnection;

	public SerialConnectionReadThread(SerialConnection serialConnection) {
		this.serialConnection = serialConnection;
	}

	@Override
	public void run() {
		Thread.currentThread().setName("SerialConnectionReadThread");
		SerialPortDataListenerImpl serialPortDataListenerImpl = new SerialPortDataListenerImpl(serialConnection.getSettings());
		serialConnection.getSerialPort().addDataListener(serialPortDataListenerImpl);

		while (true) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				log.error("Error in SerialConnectionReadThread: {}", e.getMessage());
			}
		}



	}

}
