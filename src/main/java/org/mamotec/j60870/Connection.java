package org.mamotec.j60870;

import org.mamotec.j60870.ie.IeTime56;

import java.io.IOException;
import java.util.EventListener;

/**
 * Interface for a connection to a IEC 60870-5-104 server.
 */
public interface Connection {

	void open() throws IOException;

	void close() throws IOException;

	void startDataTransfer(EventListener eventListener) throws IOException;

	void synchronizeClocks(int commonAddress, IeTime56 time) throws IOException;

	void send(ASdu aSdu) throws IOException;

}
