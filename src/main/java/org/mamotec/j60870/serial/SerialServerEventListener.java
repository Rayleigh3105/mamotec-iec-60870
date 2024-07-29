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

import java.io.IOException;
import java.util.EventListener;

public interface SerialServerEventListener extends EventListener {

    /**
     * Use this function to set the connection event listener before the connection is started. This eliminates the
     * possibility of transactions without a listener set.
     *
     * @return the ConnectionEventListener that will be set for a connection before the connection starts.
     */
    default SerialConnectionEventListener setConnectionEventListenerBeforeStart() {
        return null;
    }

    void connectionIndication(SerialConnection serialConnection);

    /**
     * This function is only called when an IOException in ServerSocket.accept() occurred which was not forced using
     * ServerSap.stopListening()
     *
     * @param e The IOException caught form ServerSocket.accept()
     */
    void serverStoppedListeningIndication(IOException e);

    void connectionAttemptFailed(IOException e);

}
