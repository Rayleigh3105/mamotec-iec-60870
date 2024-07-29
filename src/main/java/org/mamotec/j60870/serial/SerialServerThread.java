/*
 * Copyright 2014-2023 Fraunhofer ISE
 *
 * This file is part of j60870.
 * For more information visit http://www.openmuc.org
 *
 * j60870 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * j60870 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with j60870.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.mamotec.j60870.serial;

import com.fazecast.jSerialComm.SerialPort;
import org.mamotec.j60870.ASdu;
import org.mamotec.j60870.tcp.TcpConnection;
import org.mamotec.j60870.tcp.TcpConnectionSettings;
import org.mamotec.j60870.tcp.TcpServerEventListener;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;

class SerialServerThread implements Runnable {

	private final SerialPort serialPort;

	private final SerialConnectionSettings settings;

	private final int maxConnections;

	private final SerialServerEventListener serverSapListener;

	private final List<String> allowedClientIps;

	private final ExecutorService executor;

	private volatile boolean stopServer = false;

	private int numConnections = 0;

	SerialServerThread(SerialPort serialPort, SerialConnectionSettings settings, int maxConnections, SerialServerEventListener serverSapListener, ExecutorService exec,
			List<String> allowedClientIps) {
		this.serialPort = serialPort;
		this.settings = settings;
		this.maxConnections = maxConnections;
		this.serverSapListener = serverSapListener;
		this.executor = exec;
		this.allowedClientIps = allowedClientIps;
	}

	@Override
	public void run() {
		Thread.currentThread().setName("ServerThread");

		try {
			serialPort.openPort();
			serialPort.setComPortParameters(settings.getBaudRate(), 8, 1, SerialPort.EVEN_PARITY);
			serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);

			while (!stopServer) {
				if (serialPort.bytesAvailable() > 0) {
					byte[] readBuffer = new byte[serialPort.bytesAvailable()];
					int numRead = serialPort.readBytes(readBuffer, readBuffer.length);
					// Verarbeiten Sie die gelesenen Daten

				}

				if (allowedClientIps != null ) {
					serialPort.closePort();
					continue;
				}

				boolean startConnection = false;

				synchronized (this) {
					if (numConnections < maxConnections) {
						numConnections++;
						startConnection = true;
					}
				}

				if (startConnection) {
					ConnectionHandler connectionHandler = new ConnectionHandler(serialPort, this);
					executor.execute(connectionHandler);
				} else {
					serverSapListener.connectionAttemptFailed(
							new IOException("Maximum number of connections reached. Ignoring connection request. Maximum number of connections: " + maxConnections));
					serialPort.closePort();
				}

			}
		} finally {
			if (serialPort.isOpen()) {
				serialPort.closePort();
			}
		}
	}

	void connectionClosedSignal() {
		synchronized (this) {
			numConnections--;
		}
	}

	/**
	 * Stops listening for new connections. Existing connections are not touched.
	 */
	void stopServer() {
		stopServer = true;
		if (serialPort.isOpen()) {
			serialPort.closePort();
		}
	}

	private class ConnectionHandler implements Runnable {

		private final SerialPort serialPort;

		private final SerialServerThread tcpServerThread;

		public ConnectionHandler(SerialPort port, SerialServerThread tcpServerThread) {
			this.serialPort = port;
			this.tcpServerThread = tcpServerThread;
		}

		@Override
		public void run() {
			Thread.currentThread().setName("ConnectionHandler");
			SerialConnection serverSerialConnection;
			try {
				serverSerialConnection = new SerialConnection(settings);
				// first set listener before any communication can happen
				serverSerialConnection.setConnectionListener(serverSapListener.setConnectionEventListenerBeforeStart());
				serverSerialConnection.start();
			} catch (IOException e) {
				synchronized (SerialServerThread.this) {
					numConnections--;
				}
				serverSapListener.connectionAttemptFailed(e);
				return;
			}
			serverSapListener.connectionIndication(serverSerialConnection);
		}
	}

}
