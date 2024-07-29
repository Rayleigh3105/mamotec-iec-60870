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
package org.mamotec.j60870;

import org.mamotec.j60870.serial.SerialBuilder;
import org.mamotec.j60870.serial.SerialConnection;
import org.mamotec.j60870.serial.SerialConnectionSettings;

import java.io.IOException;

/**
 * The client connection builder is used to connect to IEC 60870-5-104 servers. A client application that wants to
 * connect to a server should first create an instance of {@link ClientSerialConnectionBuilder}. Next all the necessary
 * configuration parameters can be set. Finally the {@link ClientSerialConnectionBuilder#build()} function is called to
 * connect to the server. An instance of {@link ClientSerialConnectionBuilder} can be used to create an unlimited number of
 * connections. Changing the parameters of a {@link ClientSerialConnectionBuilder} has no affect on connections that have
 * already been created.
 *
 * <p>
 * Note that the configured lengths of the fields COT, CA and IOA have to be the same for all communicating nodes in a
 * network. The default values used by {@link ClientSerialConnectionBuilder} are those most commonly used in IEC 60870-5-104
 * communication.
 * </p>
 */
public class ClientSerialConnectionBuilder extends SerialBuilder<ClientSerialConnectionBuilder, SerialConnection> {

    private static final String DEFAULT_PORT = "/dev/tty.usbserial-FTB6SPL3";

    private String port = DEFAULT_PORT;


    /**
     * Creates a client connection builder that can be used to connect to the given port.
     */
    public ClientSerialConnectionBuilder(String port) {
        this.port = port;
    }

    /**
     * Sets an implementation of the ReservedASduTypeDecoder to define supported reserved ASdus
     *
     * @param reservedASduTypeDecoder implementation of the ReservedASduTypeDecoder
     */
    public ClientSerialConnectionBuilder setReservedASduTypeDecoder(ReservedASduTypeDecoder reservedASduTypeDecoder) {
        this.settings.setReservedASduTypeDecoder(reservedASduTypeDecoder);
        return this;
    }

    /**
     * Sets the port to connect to. The default port is 2404.
     *
     * @param port the port to connect to.
     * @return this builder
     */
    public ClientSerialConnectionBuilder setPort(String port) {
        this.port = port;
        return this;
    }


    /**
     * Sets connection time out t0, in milliseconds.<br>
     * t0 (connectionTimeout) must be between 1000ms and 255000ms.
     *
     * @param time_t0 the timeout in milliseconds. Default is 20 s
     * @return this builder
     */
    public ClientSerialConnectionBuilder setConnectionTimeout(int time_t0) {
        if (time_t0 < 1000 || time_t0 > 255000) {
            throw new IllegalArgumentException(
                    "invalid timeout: " + time_t0 + ", t0 (connectionTimeout) must be between 1000ms and 255000ms");
        }
        return this;
    }

    /**
     * Connects to the server. The TCP/IP connection is build up and a {@link Connection} object is returned that can be
     * used to communicate with the server.
     *
     * @return the {@link Connection} object that can be used to communicate with the server.
     * @throws IOException if any kind of error occurs during connection build up.
     */
    @Override
    public SerialConnection build() throws IOException {
        SerialConnection connection = new SerialConnection(new SerialConnectionSettings(settings));
        connection.open();

        connection.start();


        return connection;
    }

}
