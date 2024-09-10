package org.mamotec.responder.client;

import org.mamotec.j60870.ASdu;
import org.mamotec.j60870.serial.SerialConnection;
import org.mamotec.j60870.serial.SerialConnectionEventListener;

import java.io.IOException;

public class IecClientEventListenerSerial implements SerialConnectionEventListener {

	private static final String CLIENT = "CLIENT";

	@Override
	public void newASdu(SerialConnection tcpConnection, ASdu aSdu) {

	}

	@Override
	public void connectionClosed(SerialConnection tcpConnection, IOException e) {
	}

	@Override
	public void dataTransferStateChanged(SerialConnection tcpConnection, boolean stopped) {
	}

}