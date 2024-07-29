package org.mamotec.j60870.serial;

import org.mamotec.j60870.ASdu;
import org.mamotec.j60870.Connection;
import org.mamotec.j60870.ie.IeTime56;
import org.mamotec.responder.client.IecClientEventListenerSerial;
import org.mamotec.responder.server.IecSerialServerListener;

import java.io.IOException;
import java.util.EventListener;

public class SerialConnectionImpl implements Connection {

	private final SerialConnection serialConnection;

	public SerialConnectionImpl(SerialConnectionSettings settings) throws IOException {
		serialConnection = new SerialConnection(settings);
	}

	public void start() throws IOException {
		serialConnection.start(new IecSerialServerListener());
	}

	@Override
	public void open() throws IOException {
		serialConnection.open();
	}

	@Override
	public void close() throws IOException {
		serialConnection.close();
	}

	@Override
	public void startDataTransfer(EventListener eventListener) throws IOException {
		serialConnection.startDataTransfer(new IecClientEventListenerSerial());
	}

	@Override
	public void synchronizeClocks(int commonAddress, IeTime56 time) throws IOException {
		serialConnection.synchronizeClocks(new IeTime56(System.currentTimeMillis()));
	}

	@Override
	public void send(ASdu aSdu) throws IOException {
		serialConnection.send(aSdu);
	}

}
