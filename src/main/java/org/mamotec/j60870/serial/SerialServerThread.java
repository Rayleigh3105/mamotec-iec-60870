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

class SerialServerThread implements Runnable {

	private final SerialPort serialPort;

	private final SerialConnectionSettings settings;

	private volatile boolean stopServer = false;

	SerialServerThread(SerialPort serialPort, SerialConnectionSettings settings) {
		this.serialPort = serialPort;
		this.settings = settings;
	}

	@Override
	public void run() {
		Thread.currentThread().setName("ServerThread");

		try {
			while (!stopServer) {
				if (serialPort.bytesAvailable() > 0) {
					byte[] readBuffer = new byte[serialPort.bytesAvailable()];
					int numRead = serialPort.readBytes(readBuffer, readBuffer.length);
					// Verarbeiten Sie die gelesenen Daten

				}

			}
		} finally {
			if (serialPort.isOpen()) {
				serialPort.closePort();
			}
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

}
