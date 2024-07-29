package org.mamotec.responder.client;

import org.mamotec.j60870.ASdu;
import org.mamotec.j60870.serial.SerialConnection;
import org.mamotec.j60870.serial.SerialConnectionEventListener;
import org.mamotec.j60870.tcp.TcpConnection;
import org.mamotec.j60870.tcp.TcpConnectionEventListener;

import java.io.IOException;

import static org.mamotec.common.Logger.logPrefix;

public class IecClientEventListenerSerial implements SerialConnectionEventListener {

	private static final String CLIENT = "CLIENT";

	@Override
	public void newASdu(SerialConnection tcpConnection, ASdu aSdu) {
		logPrefix(CLIENT, "\nReceived ASDU:\n" + aSdu.toString());
		switch (aSdu.getTypeIdentification()) {
		case M_SP_TB_1:
			logPrefix(CLIENT, "30 - Single-point information with time tag CP56Time2a: " + aSdu.getInformationObjects()[0]);
			break;
		case M_DP_TB_1:
			logPrefix(CLIENT, "31 - Double-point information with time tag CP56Time2a: " + aSdu.getInformationObjects()[0]);
			break;
		case M_ME_TF_1:
			logPrefix(CLIENT, "36 - Measured value, short floating point number with time tag CP56Time2a: " + aSdu.getInformationObjects()[0]);
			break;
		case C_SC_NA_1:
			logPrefix(CLIENT, "45 - Double command: " + aSdu.getInformationObjects()[0]);
			break;
		case C_SE_NC_1:
			logPrefix(CLIENT, "50 - Set point command, short floating point number: " + aSdu.getInformationObjects()[0]);
			break;
		default:
			logPrefix(CLIENT, "Unknown ASDU type: " + aSdu.getTypeIdentification());
		}

	}

	@Override
	public void connectionClosed(SerialConnection tcpConnection, IOException e) {
		logPrefix(CLIENT, "Received connection closed signal. Reason: " + (e.getMessage().isEmpty() ? "unknown" : e.getMessage()));
	}

	@Override
	public void dataTransferStateChanged(SerialConnection tcpConnection, boolean stopped) {
		logPrefix(CLIENT, "Data transfer was " + (stopped ? "stopped" : "started"));
	}

}