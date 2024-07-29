package org.mamotec.responder.server;

import org.mamotec.common.Logger;
import org.mamotec.j60870.ASdu;
import org.mamotec.j60870.ASduType;
import org.mamotec.j60870.CauseOfTransmission;
import org.mamotec.j60870.tcp.TcpConnection;
import org.mamotec.j60870.tcp.TcpConnectionEventListener;
import org.mamotec.j60870.ie.IeQuality;
import org.mamotec.j60870.ie.IeScaledValue;
import org.mamotec.j60870.ie.IeSingleCommand;
import org.mamotec.j60870.ie.IeTime56;
import org.mamotec.j60870.ie.InformationElement;
import org.mamotec.j60870.ie.InformationObject;

import java.io.EOFException;
import java.io.IOException;

import static org.mamotec.common.Logger.log;
import static org.mamotec.common.Logger.logPrefix;

public class IecServerTcpConnectionListener implements TcpConnectionEventListener {

	private final TcpConnection tcpConnection;

	private final int connectionId;

	private boolean selected = false;

	private static final String SERVER = "SERVER";


	public IecServerTcpConnectionListener(TcpConnection tcpConnection, int connectionId) {
		this.tcpConnection = tcpConnection;
		this.connectionId = connectionId;
	}

	@Override
	public void newASdu(TcpConnection tcpConnection, ASdu aSdu) {
		logPrefix(SERVER, "Got new ASdu:");
		logPrefix(SERVER, aSdu.toString());
		Logger.printlnPrefix(SERVER, aSdu.toString(), "\n");
		InformationObject informationObject = null;
		try {
			switch (aSdu.getTypeIdentification()) {
			// interrogation command
			case C_IC_NA_1:
				log("Got interrogation command (100). Will send scaled measured values.");
				tcpConnection.sendConfirmation(aSdu);
				// example GI response values
				tcpConnection.send(new ASdu(ASduType.M_ME_NB_1, true, CauseOfTransmission.INTERROGATED_BY_STATION, false, false, 0, aSdu.getCommonAddress(), new InformationObject(1,
						new InformationElement[][] { { new IeScaledValue(-32768), new IeQuality(false, false, false, false, false) },
								{ new IeScaledValue(10), new IeQuality(false, false, false, false, false) },
								{ new IeScaledValue(-5), new IeQuality(false, false, false, false, false) } })));
				tcpConnection.sendActivationTermination(aSdu);
				break;
			case C_SC_NA_1:
				informationObject = aSdu.getInformationObjects()[0];
				IeSingleCommand singleCommand = (IeSingleCommand) informationObject.getInformationElements()[0][0];

				if (informationObject.getInformationObjectAddress() != 5000) {
					break;
				}
				if (singleCommand.isSelect()) {
					logPrefix(SERVER, "Got single command (45) with select true. Select command.");
					selected = true;
					tcpConnection.sendConfirmation(aSdu);
				} else if (!singleCommand.isSelect() && selected) {
					logPrefix(SERVER, "Got single command (45) with select false. Execute selected command.");
					selected = false;
					tcpConnection.sendConfirmation(aSdu);
				} else {
					logPrefix(SERVER, "Got single command (45) with select false. But no command is selected, no execution.");
				}
				break;
			case C_CS_NA_1:
				IeTime56 ieTime56 = new IeTime56(System.currentTimeMillis());
				logPrefix(SERVER, "Got Clock synchronization command (103). Send current time: \n", ieTime56.toString());
				tcpConnection.synchronizeClocks(aSdu.getCommonAddress(), ieTime56);
				break;
			case C_SE_NB_1:
				logPrefix(SERVER, "Got Set point command, scaled value (49)");
				break;
			default:
				logPrefix(SERVER, "Got unknown request: ", aSdu.toString(), ". Send negative confirm with CoT UNKNOWN_TYPE_ID(44)\n");
				tcpConnection.sendConfirmation(aSdu, aSdu.getCommonAddress(), true, CauseOfTransmission.UNKNOWN_TYPE_ID);
			}

		} catch (EOFException e) {
			logPrefix(SERVER, "Will quit listening for commands on connection (" + connectionId, ") because socket was closed.");
		} catch (IOException e) {
			logPrefix(SERVER, "Will quit listening for commands on connection (" + connectionId, ") because of error: \"", e.getMessage(), "\".");
		}

	}

	@Override
	public void connectionClosed(TcpConnection tcpConnection, IOException cause) {
		logPrefix(SERVER, "Connection (" + connectionId, ") was closed. ", cause.getMessage());

	}

	@Override
	public void dataTransferStateChanged(TcpConnection tcpConnection, boolean stopped) {
		String dtState = "started";
		if (stopped) {
			dtState = "stopped";
		}
		logPrefix(SERVER, "Data transfer of connection (" + connectionId + ") was ", dtState, ".");
	}
}
