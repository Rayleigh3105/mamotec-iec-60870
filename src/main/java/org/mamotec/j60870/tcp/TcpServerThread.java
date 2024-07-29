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
package org.mamotec.j60870.tcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;

class TcpServerThread implements Runnable {

    private final ServerSocket serverSocket;
    private final TcpConnectionSettings settings;
    private final int maxConnections;
    private final TcpServerEventListener serverSapListener;
    private final List<String> allowedClientIps;
    private final ExecutorService executor;
    private volatile boolean stopServer = false;
    private int numConnections = 0;

    TcpServerThread(ServerSocket serverSocket, TcpConnectionSettings settings, int maxConnections,
                 TcpServerEventListener serverSapListener, ExecutorService exec, List<String> allowedClientIps) {
        this.serverSocket = serverSocket;
        this.settings = settings;
        this.maxConnections = maxConnections;
        this.serverSapListener = serverSapListener;
        this.executor = exec;
        this.allowedClientIps = allowedClientIps;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("ServerThread");
        Socket clientSocket = null;

        while (!stopServer) {
            try {
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                if (!stopServer) {
                    serverSapListener.serverStoppedListeningIndication(e);
                }
                return;
            }
            if (allowedClientIps != null
                    && !allowedClientIps.contains(clientSocket.getInetAddress().getHostAddress())) {
                try {
                    clientSocket.close();
                } catch (IOException ignored) {
                    // nothing to be done if closing causes error
                }
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
                ConnectionHandler connectionHandler = new ConnectionHandler(clientSocket, this);
                executor.execute(connectionHandler);
            } else {
                serverSapListener.connectionAttemptFailed(new IOException(
                        "Maximum number of connections reached. Ignoring connection request. Maximum number of connections: "
                                + maxConnections));
                try {
                    clientSocket.close();
                } catch (IOException e) {
                }
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
        if (serverSocket.isBound()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // ignore any errors.
            }
        }
    }

    private class ConnectionHandler implements Runnable {

        private final Socket socket;
        private final TcpServerThread tcpServerThread;

        public ConnectionHandler(Socket socket, TcpServerThread tcpServerThread) {
            this.socket = socket;
            this.tcpServerThread = tcpServerThread;
        }

        @Override
        public void run() {
            Thread.currentThread().setName("ConnectionHandler");
            TcpConnection serverTcpConnection;
            try {
                serverTcpConnection = new TcpConnection(socket, tcpServerThread, settings);
                // first set listener before any communication can happen
                serverTcpConnection.setConnectionListener(serverSapListener.setConnectionEventListenerBeforeStart());
                serverTcpConnection.start();
            } catch (IOException e) {
                synchronized (TcpServerThread.this) {
                    numConnections--;
                }
                serverSapListener.connectionAttemptFailed(e);
                return;
            }
            serverSapListener.connectionIndication(serverTcpConnection);
        }
    }

}
