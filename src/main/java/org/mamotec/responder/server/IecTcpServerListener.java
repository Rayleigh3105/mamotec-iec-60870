package org.mamotec.responder.server;

import org.mamotec.j60870.tcp.TcpConnection;
import org.mamotec.j60870.tcp.TcpServerEventListener;

import java.io.IOException;

import static org.mamotec.common.Logger.logPrefix;

public class IecTcpServerListener implements TcpServerEventListener {

	private int connectionIdCounter = 1;

	private static final String SERVER = "SERVER";

	@Override
	public void connectionIndication(TcpConnection tcpConnection) {
		int myConnectionId = connectionIdCounter++;
		logPrefix(SERVER, "A client (Originator Address " + tcpConnection.getOriginatorAddress()
				+ ") has connected using TCP/IP. Will listen for a StartDT request. Connection ID: "
				+ myConnectionId);
		logPrefix(SERVER, "Started data transfer on connection (" + myConnectionId, ") Will listen for incoming commands.");

		tcpConnection.setConnectionListener(new IecServerTcpConnectionListener(tcpConnection, myConnectionId));
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
