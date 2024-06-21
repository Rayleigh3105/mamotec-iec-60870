package org.mamotec.responder.client;

import org.mamotec.j60870.ASdu;
import org.mamotec.j60870.Connection;
import org.mamotec.j60870.ConnectionEventListener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ClientEventListener implements ConnectionEventListener {

	@Override
	public void newASdu(Connection connection, ASdu aSdu) {
		log("\nReceived ASDU:\n" + aSdu.toString());
	}

	@Override
	public void connectionClosed(Connection connection, IOException e) {
		log("Received connection closed signal. Reason: " + (e.getMessage().isEmpty() ? "unknown" : e.getMessage()));
	}

	@Override
	public void dataTransferStateChanged(Connection connection, boolean stopped) {
		log("Data transfer was " + (stopped ? "stopped" : "started"));
	}

	private void log(String message) {
		String time = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS ").format(new Date());
		System.out.println(time + message);
	}
}