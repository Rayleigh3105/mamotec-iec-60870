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
package org.mamotec.j60870.ie;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mamotec.j60870.*;
import org.mamotec.j60870.internal.ExtendedDataInputStream;
import org.mamotec.j60870.tcp.TcpConnection;
import org.mamotec.j60870.tcp.TcpConnectionEventListener;
import org.mamotec.j60870.tcp.TcpServer;
import org.mamotec.j60870.tcp.TcpServerEventListener;

import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;
import static org.mamotec.j60870.IecClientTcpServerITest.getAvailablePort;

public class TransmissionControlUsingStartStopTest {

    TcpServer tcpServerSap;
    TcpConnection clientTcpConnection;
    TcpConnection serverTcpConnection;
    ClientTcpConnectionListenerImpl clientConnectionListener;
    ServerTcpConnectionListenerImpl serverConnectionListener;
    IOException clientStoppedCause;
    IOException serverStoppedCause;
    boolean newASduCalled;
    CountDownLatch connectionWaitLatch;

    @Before
    public void initConnection() throws IOException, InterruptedException {
        int port = getAvailablePort();
        newASduCalled = false;
        clientConnectionListener = new ClientTcpConnectionListenerImpl();
        serverConnectionListener = new ServerTcpConnectionListenerImpl();
        TcpServerListenerImpl serverListener = new TcpServerListenerImpl();
        connectionWaitLatch = new CountDownLatch(1);
        tcpServerSap = TcpServer.builder().setPort(port).build();
        tcpServerSap.start(serverListener);
        clientTcpConnection = new ClientConnectionBuilder("127.0.0.1")
                .setPort(port)
                .setReservedASduTypeDecoder(new ReservedASduTypeDecoderImpl())
                .build();
        clientTcpConnection.setConnectionListener(clientConnectionListener);
        connectionWaitLatch.await();
    }

    @After
    public void exitConnection() {
        clientTcpConnection.close();
        tcpServerSap.stop();
    }

    /***
     * 5.3.2.70 Description block 2. Expect Active Close on receipt of I- or S-frames.
     */
    @Test
    public void receiveIorSFramesInStoppedConnectionState()
            throws InterruptedException, IOException, NoSuchFieldException, IllegalAccessException {
        Field field = TcpConnection.class.getDeclaredField("stopped");
        field.setAccessible(true);
        field.set(clientTcpConnection, false);
        clientTcpConnection.interrogation(1, CauseOfTransmission.ACTIVATION, new IeQualifierOfInterrogation(20));
        Thread.sleep(1000);
        assertTrue(serverTcpConnection.isClosed());
        assertTrue(clientTcpConnection.isClosed());
        assertFalse(newASduCalled);
        assertEquals(serverStoppedCause.getClass(), IOException.class);
        assertTrue(serverStoppedCause.getMessage().contains("message while STOPDT state"));
        // controlled station (server) closes because it receives an ASdu while in stopped state, thus controller
        // throws EOFException because remote closed
        Thread.sleep(1000);
        assertEquals(EOFException.class, clientStoppedCause.getClass());
        assertTrue(clientStoppedCause.getMessage().contains("Connection was closed by remote."));
    }

    @Test
    public void receiveIorSFramesInStoppedConnectionStateAfterStartAndStop()
            throws InterruptedException, IOException, NoSuchFieldException, IllegalAccessException {
        clientTcpConnection.startDataTransfer(clientConnectionListener);
        clientTcpConnection.stopDataTransfer();
        Field field = TcpConnection.class.getDeclaredField("stopped");
        field.setAccessible(true);
        field.set(clientTcpConnection, false); // overwrite to send illegal message anyway
        clientTcpConnection.interrogation(1, CauseOfTransmission.ACTIVATION, new IeQualifierOfInterrogation(20));
        Thread.sleep(1000);
        assertTrue(serverTcpConnection.isClosed());
        assertTrue(clientTcpConnection.isClosed());
        assertFalse(newASduCalled);
        assertEquals(serverStoppedCause.getClass(), IOException.class);
        assertTrue(serverStoppedCause.getMessage().contains("message while STOPDT state"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwExceptionOnSendInStoppedConnectionStateBeforeStart() throws IOException {
        clientTcpConnection.interrogation(1, CauseOfTransmission.ACTIVATION, new IeQualifierOfInterrogation(20));
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwExceptionOnSendInStoppedConnectionStateAfterStop() throws IOException {
        clientTcpConnection.startDataTransfer(clientConnectionListener);
        clientTcpConnection.stopDataTransfer();
        clientTcpConnection.interrogation(1, CauseOfTransmission.ACTIVATION, new IeQualifierOfInterrogation(20));
    }

    @Test
    public void sendNoneStandardASdu() throws IOException, InterruptedException {
        clientTcpConnection.startDataTransfer(clientConnectionListener);
        serverTcpConnection.send(new ASdu(ASduType.PRIVATE_136,
                        false,
                        1,
                        CauseOfTransmission.SPONTANEOUS,
                        false,
                        false,
                        0,
                        10,
                        new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9}
                )
        );
        Thread.sleep(1000);
        assertTrue(newASduCalled);
    }

    private static class ReservedASduTypeDecoderImpl implements ReservedASduTypeDecoder {

        @Override
        public List<ASduType> getSupportedTypes() {
            List<ASduType> supported = new ArrayList<>();
            supported.add(ASduType.RESERVED_44);
            return supported;
        }

        @Override
        public InformationObject decode(ExtendedDataInputStream is, ASduType aSduType) {
            return new InformationObject(0, new InformationElement() {
                @Override
                int encode(byte[] buffer, int i) {
                    return 0;
                }

                @Override
                public String toString() {
                    return "someString";
                }
            });
        }
    }

    private class ClientTcpConnectionListenerImpl implements TcpConnectionEventListener {

        @Override
        public void newASdu(TcpConnection tcpConnection, ASdu aSdu) {
            newASduCalled = true;
        }

        @Override
        public void connectionClosed(TcpConnection tcpConnection, IOException cause) {
            clientStoppedCause = cause;
        }

        @Override
        public void dataTransferStateChanged(TcpConnection tcpConnection, boolean stopped) {

        }
    }

    private class ServerTcpConnectionListenerImpl implements TcpConnectionEventListener {

        @Override
        public void newASdu(TcpConnection tcpConnection, ASdu aSdu) {
            newASduCalled = true;
        }

        @Override
        public void connectionClosed(TcpConnection tcpConnection, IOException cause) {
            serverStoppedCause = cause;
        }

        @Override
        public void dataTransferStateChanged(TcpConnection tcpConnection, boolean stopped) {

        }
    }

    private class TcpServerListenerImpl implements TcpServerEventListener {

        @Override
        public TcpConnectionEventListener setConnectionEventListenerBeforeStart() {
            return serverConnectionListener;
        }

        @Override
        public void connectionIndication(TcpConnection tcpConnection) {
            serverTcpConnection = tcpConnection;
            connectionWaitLatch.countDown();
        }

        @Override
        public void serverStoppedListeningIndication(IOException e) {

        }

        @Override
        public void connectionAttemptFailed(IOException e) {

        }
    }
}
