package org.mamotec.responder.server;

import org.mamotec.j60870.serial.SerialConnection;
import org.mamotec.j60870.serial.SerialServerEventListener;
import org.mamotec.responder.client.IecClientEventListenerSerial;

import java.io.IOException;

import static org.mamotec.common.Logger.logPrefix;

public class IecSerialServerListener implements SerialServerEventListener {

	private int connectionIdCounter = 1;

	private static final String SERVER = "SERVER";

	@Override
	public void connectionIndication(SerialConnection serialConnection) {
		int myConnectionId = connectionIdCounter++;
		logPrefix(SERVER, "A client (Originator Address " + serialConnection.getOriginatorAddress()
				+ ") has connected using TCP/IP. Will listen for a StartDT request. Connection ID: "
				+ myConnectionId);
		logPrefix(SERVER, "Started data transfer on connection (" + myConnectionId, ") Will listen for incoming commands.");

		serialConnection.setConnectionListener(new IecClientEventListenerSerial());
	}

	@Override
	public void serverStoppedListeningIndication(IOException e) {
		logPrefix(SERVER, "Server has stopped listening for new connections : \"", e.getMessage(), "\". Will quit.");
	}

	@Override
	public void connectionAttemptFailed(IOException e) {
		logPrefix(SERVER, "Connection attempt failed: ", e.getMessage());
	}
}
