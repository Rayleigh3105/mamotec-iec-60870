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
import org.mamotec.j60870.ReservedASduTypeDecoder;
import org.mamotec.j60870.tcp.TcpConnectionSettings;
import org.mamotec.j60870.tcp.TcpServerEventListener;

import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The server is used to start listening for IEC 60870-5-104 client connections.
 */
public class SerialServer {

	private final String port;

	private final int backlog;

	private final int maxConnections;

	private final List<String> allowedClientIps;

	private final SerialConnectionSettings settings;

	private SerialServerThread serialServerThread;

	private ExecutorService exec;

	private SerialServer(Builder builder) {
		port = builder.port;
		backlog = builder.backlog;
		maxConnections = builder.maxConnections;
		allowedClientIps = builder.allowedClientIps;
		settings = new SerialConnectionSettings(builder.settings);
	}

	public static Builder builder() {
		return new Builder();
	}

	public boolean isStopped() {
		return serialServerThread == null;
	}

	/**
	 * Starts a new thread that listens on the configured port. This method is non-blocking.
	 *
	 * @param listener the ServerConnectionListener that will be notified when remote clients are connecting or the server
	 *                 stopped listening.
	 * @throws IOException if any kind of error occurs while creating the server socket.
	 */
	public void start(SerialServerEventListener listener) throws IOException {
		this.exec = Executors.newCachedThreadPool();
		SerialPort serialPort = SerialPort.getCommPort(port);
		serialServerThread = new SerialServerThread(serialPort, settings, maxConnections, listener, exec, allowedClientIps);
		this.exec.execute(this.serialServerThread);
	}

	/**
	 * Stop listening for new connections. Existing connections are not touched.
	 */
	public void stop() {
		if (serialServerThread == null) {
			return;
		}

		serialServerThread.stopServer();

		if (this.settings.useSharedThreadPool()) {
			TcpConnectionSettings.decrementConnectionsCounter();
		} else {
			this.exec.shutdown();
		}

		serialServerThread = null;
	}

	/**
	 * The server builder which builds a 60870 server instance.
	 *
	 * @see SerialServer#builder()
	 */
	public static class Builder extends SerialBuilder<Builder, SerialServer> {

		private String port = "2404";


		private int backlog = 0;

		private ServerSocketFactory serverSocketFactory = ServerSocketFactory.getDefault();

		private List<String> allowedClientIps = null;

		private int maxConnections = 100;

		private Builder() {
		}

		/**
		 * Sets the TCP port that the server will listen on. IEC 60870-5-104 usually uses port 2404.
		 *
		 * @param port the port
		 * @return this builder
		 */
		public Builder setPort(String port) {
			this.port = port;
			return this;
		}

		/**
		 * Sets the backlog that is passed to the java.net.ServerSocket.
		 *
		 * @param backlog the backlog
		 * @return this builder
		 */
		public Builder setBacklog(int backlog) {
			this.backlog = backlog;
			return this;
		}

		/**
		 * Sets an implementation of the ReservedASduTypeDecoder to define supported reserved ASdus
		 *
		 * @param reservedASduTypeDecoder implementation of the ReservedASduTypeDecoder
		 */
		public void setReservedASduTypeDecoder(ReservedASduTypeDecoder reservedASduTypeDecoder) {
			this.settings.setReservedASduTypeDecoder(reservedASduTypeDecoder);
		}

		/**
		 * Sets the ServerSocketFactory to be used to create the ServerSocket. Default is
		 * ServerSocketFactory.getDefault().
		 *
		 * @param socketFactory the ServerSocketFactory to be used to create the ServerSocket
		 * @return this builder
		 */
		public Builder setSocketFactory(ServerSocketFactory socketFactory) {
			this.serverSocketFactory = socketFactory;
			return this;
		}

		/**
		 * Set the maximum number of client connections that are allowed in parallel.
		 *
		 * @param maxConnections the number of connections allowed (default is 100) @ return this builder
		 * @return this builder
		 */
		public Builder setMaxConnections(int maxConnections) {
			if (maxConnections <= 0) {
				throw new IllegalArgumentException("maxConnections is out of bound");
			}
			this.maxConnections = maxConnections;
			return this;
		}

		/**
		 * Set the IPs from which clients may connect. Pass {@code null} to allow all clients. By default all clients
		 * are allowed to connect.
		 *
		 * @param allowedClientIps the allowed client IPs
		 * @return this builder
		 */
		public Builder setAllowedClients(List<String> allowedClientIps) {
			this.allowedClientIps = allowedClientIps;
			return this;
		}

		/**
		 * To start/activate the server call {@link SerialServer#start(TcpServerEventListener)} on the returned server.
		 */
		@Override
		public SerialServer build() {
			return new SerialServer(this);
		}

	}

}
