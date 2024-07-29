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
import org.mamotec.j60870.APdu;
import org.mamotec.j60870.APdu.ApciType;
import org.mamotec.j60870.ASdu;
import org.mamotec.j60870.ASduType;
import org.mamotec.j60870.CauseOfTransmission;
import org.mamotec.j60870.ClientConnectionBuilder;
import org.mamotec.j60870.TimeoutManager;
import org.mamotec.j60870.TimeoutTask;
import org.mamotec.j60870.ie.IeAckFileOrSectionQualifier;
import org.mamotec.j60870.ie.IeBinaryStateInformation;
import org.mamotec.j60870.ie.IeChecksum;
import org.mamotec.j60870.ie.IeDoubleCommand;
import org.mamotec.j60870.ie.IeFileReadyQualifier;
import org.mamotec.j60870.ie.IeFileSegment;
import org.mamotec.j60870.ie.IeFixedTestBitPattern;
import org.mamotec.j60870.ie.IeLastSectionOrSegmentQualifier;
import org.mamotec.j60870.ie.IeLengthOfFileOrSection;
import org.mamotec.j60870.ie.IeNameOfFile;
import org.mamotec.j60870.ie.IeNameOfSection;
import org.mamotec.j60870.ie.IeNormalizedValue;
import org.mamotec.j60870.ie.IeQualifierOfCounterInterrogation;
import org.mamotec.j60870.ie.IeQualifierOfInterrogation;
import org.mamotec.j60870.ie.IeQualifierOfParameterActivation;
import org.mamotec.j60870.ie.IeQualifierOfParameterOfMeasuredValues;
import org.mamotec.j60870.ie.IeQualifierOfResetProcessCommand;
import org.mamotec.j60870.ie.IeQualifierOfSetPointCommand;
import org.mamotec.j60870.ie.IeRegulatingStepCommand;
import org.mamotec.j60870.ie.IeScaledValue;
import org.mamotec.j60870.ie.IeSectionReadyQualifier;
import org.mamotec.j60870.ie.IeSelectAndCallQualifier;
import org.mamotec.j60870.ie.IeShortFloat;
import org.mamotec.j60870.ie.IeSingleCommand;
import org.mamotec.j60870.ie.IeTestSequenceCounter;
import org.mamotec.j60870.ie.IeTime16;
import org.mamotec.j60870.ie.IeTime56;
import org.mamotec.j60870.ie.InformationElement;
import org.mamotec.j60870.ie.InformationObject;
import org.mamotec.j60870.internal.SerialExecutor;
import org.mamotec.j60870.tcp.TcpConnectionSettings;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Represents an open connection to a specific 60870 server. It is created either through an instance of
 * {@link ClientConnectionBuilder} or passed to {@link SerialConnectionEventListener}. Once it has been closed it cannot be opened
 * again. A newly created connection has successfully build up a TCP/IP connection to the server. Before receiving ASDUs
 * or sending commands one has to call {@link SerialConnection#startDataTransfer(SerialConnectionEventListener)}. Afterwards
 * incoming ASDUs are forwarded to the {@link SerialConnectionEventListener}. Incoming ASDUs are queued so that
 * {@link SerialConnectionEventListener#newASdu(SerialConnection connection, ASdu)} is never called simultaneously for the same connection.
 *
 * <p>
 * Connection offers a method for every possible command defined by IEC 60870 (e.g. singleCommand). Every command
 * function may throw an IOException indicating a fatal connection error. In this case the connection will be
 * automatically closed and a new connection will have to be built up. The command methods do not wait for an
 * acknowledgment but return right after the command has been sent.
 * </p>
 */
public class SerialConnection implements AutoCloseable {
    private static final byte[] TESTFR_CON_BUFFER = new byte[]{0x68, 0x04, (byte) 0x83, 0x00, 0x00, 0x00};
    private static final byte[] TESTFR_ACT_BUFFER = new byte[]{0x68, 0x04, (byte) 0x43, 0x00, 0x00, 0x00};
    private static final byte[] STARTDT_ACT_BUFFER = new byte[]{0x68, 0x04, 0x07, 0x00, 0x00, 0x00};
    private static final byte[] STARTDT_CON_BUFFER = new byte[]{0x68, 0x04, 0x0b, 0x00, 0x00, 0x00};
    private static final byte[] STOPDT_ACT_BUFFER = new byte[]{0x68, 0x04, 0x13, 0x00, 0x00, 0x00};
    private static final byte[] STOPDT_CON_BUFFER = new byte[]{0x68, 0x04, 0x23, 0x00, 0x00, 0x00};

    private final SerialPort serialPort;
    private OutputStream os;
    private InputStream is;
    private final SerialConnectionSettings settings;
    private final byte[] buffer = new byte[255];
    private volatile boolean closed;
    private volatile boolean stopped = true;
    private volatile boolean pending = false;
    private SerialConnectionEventListener aSduListener;
    private SerialConnectionEventListener aSduListenerBack;
    private int sendSequenceNumber;
    private int receiveSequenceNumber;
    private int acknowledgedReceiveSequenceNumber;
    private int acknowledgedSendSequenceNumber;
    private int originatorAddress;
    private TimeoutManager timeoutManager;

    private TimeoutTask maxTimeNoTestConReceived;
    private TimeoutTask maxTimeNoAckReceived;
    private TimeoutTask maxIdleTimeTimer;
    private TimeoutTask maxTimeNoAckSentTimer;

    private TimeoutTask stopDtOutstandingSFormatTimer;

    private IOException closedIOException;

    private CountDownLatch startDtActSignal;
    private CountDownLatch startDtConSignal;
    private CountDownLatch stopDtConSignal;

    private ExecutorService executor;
    private SerialExecutor serialExecutor;

    public SerialConnection(SerialConnectionSettings settings) throws IOException {
        this.settings = settings;
        serialPort = SerialPort.getCommPort(settings.getPortName());
        serialPort.setBaudRate(settings.getBaudRate());
        serialPort.setParity(SerialPort.EVEN_PARITY);
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(1);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);
    }

    public void open() throws IOException {
        if (serialPort.openPort()) {
            System.out.println("Port is open: " + settings.getPortName());
            is = serialPort.getInputStream();
            os = new DataOutputStream(new BufferedOutputStream(serialPort.getOutputStream()));
        } else {
            throw new IOException("Failed to open serial port " + settings.getPortName());
        }
    }

    private static int sequenceNumberDiff(int number, int ackNumber) {
        // would hold true: ackNumber <= number (without mod 2^15)
        return ackNumber > number ? ((1 << 15) - ackNumber) + number : number - ackNumber;
    }

    private void sendTestFrameCon() throws IOException {
        os.write(TESTFR_CON_BUFFER);
        os.flush();
    }

    private void closeIfStopped(ApciType apciType) throws IOException {
        if (stopped && !pending) {
            throw new IOException("Got " + apciType + " message while STOPDT state.");
        }
    }

    private void handleStopDtAct() throws IOException {
        setStopped(true);
        pending = true;
        if (aSduListener != null) {
            aSduListenerBack = aSduListener;
            aSduListener = null;
        }

        if (maxTimeNoAckSentTimer.isPlanned()) {
            maxTimeNoAckSentTimer.cancel();
        }

        sendSFormatIfUnconfirmedAPdu();

        if (sequenceNumberDiff(sendSequenceNumber, acknowledgedSendSequenceNumber) > 0
                && maxTimeNoAckReceived.isPlanned()) {
            stopDtOutstandingSFormatTimer = new StopDtOutstandingSFormatTimer(
                    maxTimeNoAckReceived.sleepTimeFromDueTime());
            timeoutManager.addTimerTask(stopDtOutstandingSFormatTimer);
        } else {
            sendStopDtCon();
        }
    }

    private void sendStopDtCon() throws IOException {
        synchronized (this) {
            os.write(STOPDT_CON_BUFFER);
            os.flush();
            pending = false;
        }
    }

    private void handleStartDtAct() throws IOException {

        synchronized (this) {
            os.write(STARTDT_CON_BUFFER, 0, STARTDT_CON_BUFFER.length);

            if (aSduListener == null) {
                aSduListener = aSduListenerBack;
            }
            setStopped(false);
        }
        os.flush();

        resetMaxIdleTimeTimer();
    }

    private void handleIFrame(final APdu aPdu) throws IOException {

        updateReceiveSeqNum(aPdu.getSendSeqNumber());

        handleReceiveSequenceNumber(aPdu.getReceiveSeqNumber());

        if (aSduListener != null) {
            serialExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    Thread.currentThread().setName("aSduListener");
                    aSduListener.newASdu(SerialConnection.this, aPdu.getASdu());
                }
            });
        }

        int numUnconfirmedIPdusReceived = sequenceNumberDiff(receiveSequenceNumber, acknowledgedReceiveSequenceNumber);

        if (numUnconfirmedIPdusReceived >= settings.getMaxUnconfirmedIPdusReceived()) {
            sendSFormatPdu();
            if (maxTimeNoAckSentTimer.isPlanned()) {
                maxTimeNoAckSentTimer.cancel();
            }
        } else if (!maxTimeNoAckSentTimer.isPlanned()
                || (maxTimeNoAckSentTimer.isPlanned() && numUnconfirmedIPdusReceived == 1)) {
            timeoutManager.addTimerTask(maxTimeNoAckSentTimer);
        }

    }

    private void updateReceiveSeqNum(int sendSeqNumber) throws IOException {
        verifySeqNumber(sendSeqNumber);

        receiveSequenceNumber = (sendSeqNumber + 1) % (1 << 15); // 32768

        // check for receiveSequenceNumber overflow
        if (sendSeqNumber > receiveSequenceNumber) {
            sendSFormatPdu();
        }
    }

    private void verifySeqNumber(int sendSeqNumber) throws IOException {
        if (receiveSequenceNumber != sendSeqNumber) {
            String msg = MessageFormat.format("Got unexpected send sequence number: {0}, expected: {1}.", sendSeqNumber,
                    receiveSequenceNumber);
            throw new IOException(msg);
        }
    }

    private void handleReceiveSequenceNumber(int receiveSeqNumber) throws IOException {
        if (acknowledgedSendSequenceNumber == receiveSeqNumber) {
            return;
        }

        int diff = sequenceNumberDiff(receiveSeqNumber, acknowledgedSendSequenceNumber);
        if (diff > getNumUnconfirmedAPdusSent()) {
            String msg = MessageFormat.format(
                    "Got unexpected receive sequence number: {0}, expected a number between: {1} and {2}.",
                    receiveSeqNumber, acknowledgedSendSequenceNumber, sendSequenceNumber);
            throw new IOException(msg);
        }

        if (maxTimeNoAckReceived.isPlanned()) {
            maxTimeNoAckReceived.cancel();
        }

        acknowledgedSendSequenceNumber = receiveSeqNumber;

        if (sendSequenceNumber != acknowledgedSendSequenceNumber) {
            if (getNumUnconfirmedAPdusSent() > settings.getMaxNumOfOutstandingIPdus()) {
                throw new IOException("Max number of outstanding IPdus is exceeded.");
            } else {
                timeoutManager.addTimerTask(maxTimeNoAckReceived);
            }
        }
        SerialConnection.this.notifyAll();
    }

    protected void start() {

        ConnectionReader connectionReader = new ConnectionReader();
        connectionReader.start();

        this.maxTimeNoTestConReceived = new MaxTimeNoAckReceivedTimer();
        this.maxTimeNoAckReceived = new MaxTimeNoAckReceivedTimer();
        this.maxIdleTimeTimer = new MaxIdleTimeTimer();
        this.maxTimeNoAckSentTimer = new MaxTimeNoAckSentTimer();

        if (settings.useSharedThreadPool()) {
            this.executor = TcpConnectionSettings.getThreadPool();
        } else {
            this.executor = Executors.newCachedThreadPool();
        }
        serialExecutor = new SerialExecutor(executor);
        TcpConnectionSettings.incremntConnectionsCounter();

        this.timeoutManager = new TimeoutManager();
        this.executor.execute(this.timeoutManager);

        // set maxIdleTimeTimer after connection is started
        this.timeoutManager.addTimerTask(maxIdleTimeTimer);
    }

    /**
     * Stops the data transfer. First sends S-Format to confirm unconfirmed I-Format messages, then sends a STOPDT act
     * and waits for a STOPDT con. If successful the data transfer stops and releases the ASduListener. If no STOPDT con
     * could be received, while t1, the connection will be closed.
     */
    public void stopDataTransfer() throws IOException {

        synchronized (this) {
            pending = true;
            aSduListenerBack = aSduListener;
            setStopped(true);
            aSduListener = null;
        }

        sendSFormatIfUnconfirmedAPdu();

        if (maxTimeNoAckSentTimer.isPlanned()) {
            maxTimeNoAckSentTimer.cancel();
        }

        synchronized (this) {
            stopDtConSignal = new CountDownLatch(1);
            os.write(STOPDT_ACT_BUFFER);
        }
        os.flush();

        boolean success;
        try {
            success = stopDtConSignal.await(settings.getMaxTimeNoAckReceived(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            success = true;
            Thread.currentThread().interrupt();
        }

        if (!success) {
            throw new InterruptedIOException("Request timed out.");
        }

        pending = false;
    }

    private void sendSFormatIfUnconfirmedAPdu() throws IOException {
        int diff;
        synchronized (this) {
            diff = sequenceNumberDiff(receiveSequenceNumber, acknowledgedReceiveSequenceNumber);
        }
        if (diff > 0) {
            sendSFormatPdu();
            if (maxTimeNoAckSentTimer.isPlanned()) {
                maxTimeNoAckSentTimer.cancel();
            }
        }
    }

    /**
     * Starts a connection. Sends a STARTDT act and waits for a STARTDT con. If successful a new thread will be started
     * that listens for incoming ASDUs and notifies the given ASduListener.
     *
     * @param listener the listener that is notified of incoming ASDUs
     * @throws IOException if any kind of IOException occurs.
     */
    public void startDataTransfer(SerialConnectionEventListener listener) throws IOException {

        synchronized (this) {
            startDtConSignal = new CountDownLatch(1);
            os.write(STARTDT_ACT_BUFFER);
        }
        os.flush();

        boolean success;
        try {
            success = startDtConSignal.await(settings.getMaxTimeNoAckReceived(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            success = true;
            Thread.currentThread().interrupt();
        }

        if (!success) {
            throw new InterruptedIOException("Request timed out.");
        }

        synchronized (this) {
            this.aSduListener = listener;
            setStopped(false);
        }
    }

    /**
     * Sets connection listener to be notified of incoming ASDUs and disconnect events.
     *
     * @param listener the listener that is to be notified of incoming ASDUs and disconnect events
     */
    public void setConnectionListener(SerialConnectionEventListener listener) {
        synchronized (this) {
            this.aSduListener = listener;
        }
    }

    private void sendSFormatPdu() throws IOException {

        int length = new APdu(0, receiveSequenceNumber, ApciType.S_FORMAT, null).encode(buffer, settings);

        os.write(buffer, 0, length);
        os.flush();

        acknowledgedReceiveSequenceNumber = receiveSequenceNumber;

        resetMaxIdleTimeTimer();
    }

    /**
     * Get the configured Originator Address.
     *
     * @return the Originator Address
     */
    public int getOriginatorAddress() {
        return originatorAddress;
    }

    /**
     * Set the Originator Address. It is the address of controlling station (client) so that responses can be routed
     * back to it. Originator addresses from 1 to 255 are used to address a particular controlling station. Address 0 is
     * the default and is used if responses are to be routed to all controlling stations in the system. Note that the
     * same Originator Address is sent in a command and its confirmation.
     *
     * @param originatorAddress the Originator Address. Valid values are 0...255.
     */
    public void setOriginatorAddress(int originatorAddress) {
        if (originatorAddress < 0 || originatorAddress > 255) {
            throw new IllegalArgumentException("Originator Address must be between 0 and 255.");
        }
        this.originatorAddress = originatorAddress;
    }

    public int getNumUnconfirmedAPdusSent() {
        synchronized (this) {
            return sequenceNumberDiff(sendSequenceNumber, acknowledgedSendSequenceNumber);
        }
    }

    @Override
    public synchronized void close() {
        if (serialPort != null) {
            serialPort.closePort();
        }
    }

    /**
     * Returns true if connection is closed else false.
     *
     * @return true if closed
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Returns true if data transfer is stopped.
     *
     * @return true if stopped
     */
    public boolean isStopped() {
        return stopped;
    }

    private void setStopped(boolean stopped) {
        this.stopped = stopped;
        if (aSduListener != null) {
            this.aSduListener.dataTransferStateChanged(SerialConnection.this, stopped);
        }
    }

    public synchronized void send(ASdu aSdu) throws IOException, IllegalArgumentException {

        while (getNumUnconfirmedAPdusSent() >= settings.getMaxNumOfOutstandingIPdus()) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                throw new IOException(e);
            }
        }

        if (closed) {
            throw new IOException("connection closed");
        }
        if (stopped) {
            throw new IllegalArgumentException("May not send ASdu, data transfer is stopped.");
        }

        acknowledgedReceiveSequenceNumber = receiveSequenceNumber;
        APdu requestAPdu = new APdu(sendSequenceNumber, receiveSequenceNumber, ApciType.I_FORMAT, aSdu);

        int oldSendSequenceNumber = sendSequenceNumber;
        sendSequenceNumber = (sendSequenceNumber + 1) % (1 << 15); // 32768 = 2^15

        // check for sendSequenceNumber overflow
        if (oldSendSequenceNumber > sendSequenceNumber) {
            sendSFormatPdu();
        }

/*
        if (this.maxTimeNoAckSentTimer.isPlanned()) {
            this.maxTimeNoAckSentTimer.cancel();
        }

        if (!this.maxTimeNoAckReceived.isPlanned()) {
            this.timeoutManager.addTimerTask(this.maxTimeNoAckReceived);
        }
*/

        int length = requestAPdu.encode(buffer, settings);
        os.write(buffer, 0, length);
        os.flush();
        resetMaxIdleTimeTimer();
    }

    private void resetMaxIdleTimeTimer() {
        this.maxIdleTimeTimer.cancel();
        this.timeoutManager.addTimerTask(maxIdleTimeTimer);
    }

    /**
     * Send response with given aSdu. Common ASDU address of given ASDU is used as station address.
     *
     * @param aSdu ASDU which response to
     * @throws IOException if a fatal communication error occurred.
     */
    public void sendConfirmation(ASdu aSdu) throws IOException {
        sendConfirmation(aSdu, aSdu.getCommonAddress(), false);
    }

    /**
     * Send response with given aSdu. Given station address is used as Common ASDU Address, if we response to broadcast
     * else given Common ASDU Address of aSdu.
     *
     * @param aSdu           ASDU which response to
     * @param stationAddress address of this station
     * @throws IOException if a fatal communication error occurred.
     */
    public void sendConfirmation(ASdu aSdu, int stationAddress) throws IOException {
        sendConfirmation(aSdu, stationAddress, false);
    }

    /**
     * Send response with given aSdu. Given station address is used as Common ASDU Address, if we response to broadcast
     * else given Common ASDU Address of aSdu.
     *
     * @param aSdu              ASDU which response to
     * @param stationAddress    address of this station
     * @param isNegativeConfirm true if it is a negative confirmation
     * @throws IOException if a fatal communication error occurred.
     */
    public void sendConfirmation(ASdu aSdu, int stationAddress, boolean isNegativeConfirm) throws IOException {
        CauseOfTransmission cot = cotFrom(aSdu);
        sendActDect(aSdu, stationAddress, cot, isNegativeConfirm);
    }

    /**
     * Send response with given aSdu. Given station address is used as Common ASDU Address, if we response to broadcast
     * else given Common ASDU Address of aSdu.
     *
     * @param aSdu              ASDU which response to
     * @param stationAddress    address of this station
     * @param isNegativeConfirm true if it is a negative confirmation
     * @param cot               Cause of transmission, for e.g. negative confirm UNKNOWN_TYPE_ID(44),
     *                          UNKNOWN_CAUSE_OF_TRANSMISSION(45), UNKNOWN_COMMON_ADDRESS_OF_ASDU(46) and
     *                          UNKNOWN_INFORMATION_OBJECT_ADDRESS(47)
     * @throws IOException if a fatal communication error occurred.
     */
    public void sendConfirmation(ASdu aSdu, int stationAddress, boolean isNegativeConfirm, CauseOfTransmission cot)
            throws IOException {
        sendActDect(aSdu, stationAddress, cot, isNegativeConfirm);
    }

    /**
     * Send activation termination with given aSdu. Given station address is used as Common ASDU Address, if we response
     * to broadcast else given Common ASDU Address of aSdu.
     *
     * @param aSdu           ASDU which response to
     * @param stationAddress address of this station
     * @throws IOException if a fatal communication error occurred.
     */
    public void sendActivationTermination(ASdu aSdu, int stationAddress) throws IOException {
        sendActDect(aSdu, stationAddress, CauseOfTransmission.ACTIVATION_TERMINATION, aSdu.isNegativeConfirm());
    }

    /**
     * Send activation termination with given aSdu. Common ASDU address of given ASDU is used as station address.
     *
     * @param aSdu ASDU which response to
     * @throws IOException if a fatal communication error occurred.
     */
    public void sendActivationTermination(ASdu aSdu) throws IOException {
        sendActivationTermination(aSdu, aSdu.getCommonAddress());
    }

    private void sendActDect(ASdu aSdu, int stationAddress, CauseOfTransmission cot, boolean isNegativeConfirm)
            throws IOException {
        int commonAddress = aSdu.getCommonAddress();

        commonAddress = setCommonAddress(stationAddress, commonAddress);

        send(new ASdu(aSdu.getTypeIdentification(), aSdu.isSequenceOfElements(), cot, aSdu.isTestFrame(),
                isNegativeConfirm, aSdu.getOriginatorAddress(), commonAddress, aSdu.getInformationObjects()));
    }

    private int setCommonAddress(int stationAddress, int commonAddress) {
        int broadcastAddress;
        if (settings.getCommonAddressFieldLength() == 2) {
            broadcastAddress = 65535;
        } else {
            broadcastAddress = 255;
        }
        if (commonAddress == broadcastAddress) {
            commonAddress = stationAddress;
        }
        return commonAddress;
    }

    private CauseOfTransmission cotFrom(ASdu aSdu) {
        CauseOfTransmission cot = aSdu.getCauseOfTransmission();
        switch (cot) {
            case ACTIVATION:
                return CauseOfTransmission.ACTIVATION_CON;
            case DEACTIVATION:
                return CauseOfTransmission.DEACTIVATION_CON;
            default:
                return cot;
        }
    }

    /**
     * Sends a single command (C_SC_NA_1, TI: 45).
     *
     * @param commonAddress            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot                      the cause of transmission. Allowed are activation and deactivation.
     * @param informationObjectAddress the information object address.
     * @param singleCommand            the command to be sent.
     * @throws IOException if a fatal communication error occurred.
     */
    public void singleCommand(int commonAddress, CauseOfTransmission cot, int informationObjectAddress,
                              IeSingleCommand singleCommand) throws IOException {
        ASdu aSdu = new ASdu(ASduType.C_SC_NA_1, false, cot, false, false, originatorAddress, commonAddress,
                new InformationObject(informationObjectAddress, singleCommand));
        send(aSdu);
    }

    /**
     * Sends a single command with time tag CP56Time2a (C_SC_TA_1, TI: 58).
     *
     * @param commonAddress            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot                      the cause of transmission. Allowed are activation and deactivation.
     * @param informationObjectAddress the information object address.
     * @param singleCommand            the command to be sent.
     * @param timeTag                  the time tag to be sent.
     * @throws IOException if a fatal communication error occurred.
     */
    public void singleCommandWithTimeTag(int commonAddress, CauseOfTransmission cot, int informationObjectAddress,
                                         IeSingleCommand singleCommand, IeTime56 timeTag) throws IOException {

        ASdu aSdu = new ASdu(ASduType.C_SC_TA_1, false, cot, false, false, originatorAddress, commonAddress,
                new InformationObject(informationObjectAddress, singleCommand, timeTag));
        send(aSdu);
    }

    /**
     * Sends a double command (C_DC_NA_1, TI: 46).
     *
     * @param commonAddress            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot                      the cause of transmission. Allowed are activation and deactivation.
     * @param informationObjectAddress the information object address.
     * @param doubleCommand            the command to be sent.
     * @throws IOException if a fatal communication error occurred.
     */
    public void doubleCommand(int commonAddress, CauseOfTransmission cot, int informationObjectAddress,
                              IeDoubleCommand doubleCommand) throws IOException {

        ASdu aSdu = new ASdu(ASduType.C_DC_NA_1, false, cot, false, false, originatorAddress, commonAddress,
                new InformationObject(informationObjectAddress, doubleCommand));
        send(aSdu);
    }

    /**
     * Sends a double command with time tag CP56Time2a (C_DC_TA_1, TI: 59).
     *
     * @param commonAddress            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot                      the cause of transmission. Allowed are activation and deactivation.
     * @param informationObjectAddress the information object address.
     * @param doubleCommand            the command to be sent.
     * @param timeTag                  the time tag to be sent.
     * @throws IOException if a fatal communication error occurred.
     */
    public void doubleCommandWithTimeTag(int commonAddress, CauseOfTransmission cot, int informationObjectAddress,
                                         IeDoubleCommand doubleCommand, IeTime56 timeTag) throws IOException {

        ASdu aSdu = new ASdu(ASduType.C_DC_TA_1, false, cot, false, false, originatorAddress, commonAddress,
                new InformationObject(informationObjectAddress, doubleCommand, timeTag));
        send(aSdu);
    }

    /**
     * Sends a regulating step command (C_RC_NA_1, TI: 47).
     *
     * @param commonAddress            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot                      the cause of transmission. Allowed are activation and deactivation.
     * @param informationObjectAddress the information object address.
     * @param regulatingStepCommand    the command to be sent.
     * @throws IOException if a fatal communication error occurred.
     */
    public void regulatingStepCommand(int commonAddress, CauseOfTransmission cot, int informationObjectAddress,
                                      IeRegulatingStepCommand regulatingStepCommand) throws IOException {

        ASdu aSdu = new ASdu(ASduType.C_RC_NA_1, false, cot, false, false, originatorAddress, commonAddress,
                new InformationObject(informationObjectAddress, regulatingStepCommand));
        send(aSdu);
    }

    /**
     * Sends a regulating step command with time tag CP56Time2a (C_RC_TA_1, TI: 60).
     *
     * @param commonAddress            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot                      the cause of transmission. Allowed are activation and deactivation.
     * @param informationObjectAddress the information object address.
     * @param regulatingStepCommand    the command to be sent.
     * @param timeTag                  the time tag to be sent.
     * @throws IOException if a fatal communication error occurred.
     */
    public void regulatingStepCommandWithTimeTag(int commonAddress, CauseOfTransmission cot,
                                                 int informationObjectAddress, IeRegulatingStepCommand regulatingStepCommand, IeTime56 timeTag)
            throws IOException {

        ASdu aSdu = new ASdu(ASduType.C_RC_TA_1, false, cot, false, false, originatorAddress, commonAddress,
                new InformationObject(informationObjectAddress, regulatingStepCommand, timeTag));
        send(aSdu);
    }

    /**
     * Sends a set-point command, normalized value (C_SE_NA_1, TI: 48).
     *
     * @param commonAddress            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot                      the cause of transmission. Allowed are activation and deactivation.
     * @param informationObjectAddress the information object address.
     * @param normalizedValue          the value to be sent.
     * @param qualifier                the qualifier to be sent.
     * @throws IOException if a fatal communication error occurred.
     */
    public void setNormalizedValueCommand(int commonAddress, CauseOfTransmission cot, int informationObjectAddress,
                                          IeNormalizedValue normalizedValue, IeQualifierOfSetPointCommand qualifier) throws IOException {

        ASdu aSdu = new ASdu(ASduType.C_SE_NA_1, false, cot, false, false, originatorAddress, commonAddress,
                new InformationObject(informationObjectAddress, normalizedValue, qualifier));

        send(aSdu);
    }

    /**
     * Sends a set-point command with time tag CP56Time2a, normalized value (C_SE_TA_1, TI: 61).
     *
     * @param commonAddress            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot                      the cause of transmission. Allowed are activation and deactivation.
     * @param informationObjectAddress the information object address.
     * @param normalizedValue          the value to be sent.
     * @param qualifier                the qualifier to be sent.executor
     * @param timeTag                  the time tag to be sent.
     * @throws IOException if a fatal communication error occurred.
     */
    public void setNormalizedValueCommandWithTimeTag(int commonAddress, CauseOfTransmission cot,
                                                     int informationObjectAddress, IeNormalizedValue normalizedValue, IeQualifierOfSetPointCommand qualifier,
                                                     IeTime56 timeTag) throws IOException {

        ASdu aSdu = new ASdu(ASduType.C_SE_TA_1, false, cot, false, false, originatorAddress, commonAddress,
                new InformationObject(informationObjectAddress, normalizedValue, qualifier, timeTag));

        send(aSdu);
    }

    /**
     * Sends a set-point command, scaled value (C_SE_NB_1, TI: 49).
     *
     * @param commonAddress            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot                      the cause of transmission. Allowed are activation and deactivation.
     * @param informationObjectAddress the information object address.
     * @param scaledValue              the value to be sent.
     * @param qualifier                the qualifier to be sent.
     * @throws IOException if a fatal communication error occurred.
     */
    public void setScaledValueCommand(int commonAddress, CauseOfTransmission cot, int informationObjectAddress,
                                      IeScaledValue scaledValue, IeQualifierOfSetPointCommand qualifier) throws IOException {

        ASduType typeId = ASduType.C_SE_NB_1;
        ASdu aSdu = new ASdu(typeId, false, cot, false, false, originatorAddress, commonAddress,
                new InformationObject(informationObjectAddress, scaledValue, qualifier));
        send(aSdu);
    }

    /**
     * Sends a set-point command with time tag CP56Time2a, scaled value (C_SE_TB_1, TI: 62).
     *
     * @param commonAddress            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot                      the cause of transmission. Allowed are activation and deactivation.
     * @param informationObjectAddress the information object address.
     * @param scaledValue              the value to be sent.
     * @param qualifier                the qualifier to be sent.
     * @param timeTag                  the time tag to be sent.
     * @throws IOException if a fatal communication error occurred.
     */
    public void setScaledValueCommandWithTimeTag(int commonAddress, CauseOfTransmission cot,
                                                 int informationObjectAddress, IeScaledValue scaledValue, IeQualifierOfSetPointCommand qualifier,
                                                 IeTime56 timeTag) throws IOException {

        ASdu aSdu = new ASdu(ASduType.C_SE_TB_1, false, cot, false, false, originatorAddress, commonAddress,
                new InformationObject(informationObjectAddress, scaledValue, qualifier, timeTag));
        send(aSdu);
    }

    /**
     * Sends a set-point command, short floating point number (C_SE_NC_1, TI: 50).
     *
     * @param commonAddress            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot                      the cause of transmission. Allowed are activation and deactivation.
     * @param informationObjectAddress the information object address.
     * @param floatVal                 the value to be sent.
     * @param qualifier                the qualifier to be sent.
     * @throws IOException if a fatal communication error occurred.
     */
    public void setShortFloatCommand(int commonAddress, CauseOfTransmission cot, int informationObjectAddress,
                                     IeShortFloat floatVal, IeQualifierOfSetPointCommand qualifier) throws IOException {

        ASdu aSdu = new ASdu(ASduType.C_SE_NC_1, false, cot, false, false, originatorAddress, commonAddress,
                new InformationObject(informationObjectAddress, floatVal, qualifier));
        send(aSdu);
    }

    /**
     * Sends a set-point command with time tag CP56Time2a, short floating point number (C_SE_TC_1, TI: 63).
     *
     * @param commonAddress            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot                      the cause of transmission. Allowed are activation and deactivation.
     * @param informationObjectAddress the information object address.
     * @param shortFloat               the value to be sent.
     * @param qualifier                the qualifier to be sent.
     * @param timeTag                  the time tag to be sent.
     * @throws IOException if a fatal communication error occurred.
     */
    public void setShortFloatCommandWithTimeTag(int commonAddress, CauseOfTransmission cot,
                                                int informationObjectAddress, IeShortFloat shortFloat, IeQualifierOfSetPointCommand qualifier,
                                                IeTime56 timeTag) throws IOException {

        ASdu aSdu = new ASdu(ASduType.C_SE_TC_1, false, cot, false, false, originatorAddress, commonAddress,
                new InformationObject(informationObjectAddress, shortFloat, qualifier, timeTag));

        send(aSdu);
    }

    /**
     * Sends a bitstring of 32 bit (C_BO_NA_1, TI: 51).
     *
     * @param commonAddress            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot                      the cause of transmission. Allowed are activation and deactivation.
     * @param informationObjectAddress the information object address.
     * @param binaryStateInformation   the value to be sent.
     * @throws IOException if a fatal communication error occurred.
     */
    public void bitStringCommand(int commonAddress, CauseOfTransmission cot, int informationObjectAddress,
                                 IeBinaryStateInformation binaryStateInformation) throws IOException {

        ASdu aSdu = new ASdu(ASduType.C_BO_NA_1, false, cot, false, false, originatorAddress, commonAddress,
                new InformationObject(informationObjectAddress, binaryStateInformation));
        send(aSdu);
    }

    /**
     * Sends a bitstring of 32 bit with time tag CP56Time2a (C_BO_TA_1, TI: 64).
     *
     * @param commonAddress            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot                      the cause of transmission. Allowed are activation and deactivation.
     * @param informationObjectAddress the information object address.
     * @param binaryStateInformation   the value to be sent.
     * @param timeTag                  the time tag to be sent.
     * @throws IOException if a fatal communication error occurred.
     */
    public void bitStringCommandWithTimeTag(int commonAddress, CauseOfTransmission cot, int informationObjectAddress,
                                            IeBinaryStateInformation binaryStateInformation, IeTime56 timeTag) throws IOException {

        ASdu aSdu = new ASdu(ASduType.C_BO_TA_1, false, cot, false, false, originatorAddress, commonAddress,
                new InformationObject(informationObjectAddress, binaryStateInformation, timeTag));
        send(aSdu);
    }

    /**
     * Sends an interrogation command (C_IC_NA_1, TI: 100).
     *
     * @param commonAddress the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot           the cause of transmission. Allowed are activation and deactivation.
     * @param qualifier     the qualifier to be sent.
     * @throws IOException if a fatal communication error occurred.
     */
    public void interrogation(int commonAddress, CauseOfTransmission cot, IeQualifierOfInterrogation qualifier)
            throws IOException {
        ASdu aSdu = new ASdu(ASduType.C_IC_NA_1, false, cot, false, false, originatorAddress, commonAddress,
                new InformationObject(0, qualifier));

        send(aSdu);
    }

    /**
     * Sends a counter interrogation command (C_CI_NA_1, TI: 101).
     *
     * @param commonAddress the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot           the cause of transmission. Allowed are activation and deactivation.
     * @param qualifier     the qualifier to be sent.
     * @throws IOException if a fatal communication error occurred.
     */
    public void counterInterrogation(int commonAddress, CauseOfTransmission cot,
                                     IeQualifierOfCounterInterrogation qualifier) throws IOException {
        ASdu aSdu = new ASdu(ASduType.C_CI_NA_1, false, cot, false, false, originatorAddress, commonAddress,
                new InformationObject(0, qualifier));
        send(aSdu);
    }

    /**
     * Sends a read command (C_RD_NA_1, TI: 102).
     *
     * @param commonAddress            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param informationObjectAddress the information object address.
     * @throws IOException if a fatal communication error occurred.
     */
    public void readCommand(int commonAddress, int informationObjectAddress) throws IOException {
        ASdu aSdu = new ASdu(ASduType.C_RD_NA_1, false, CauseOfTransmission.REQUEST, false, false, originatorAddress,
                commonAddress, new InformationObject(informationObjectAddress));
        send(aSdu);
    }

    /**
     * Sends a clock synchronization command (C_CS_NA_1, TI: 103).
     *
     * @param time          the time to be sent.
     * @throws IOException if a fatal communication error occurred.
     */
    public void synchronizeClocks(IeTime56 time) throws IOException {
        InformationObject io = new InformationObject(0, time);

        ASdu aSdu = new ASdu(ASduType.C_CS_NA_1, false, CauseOfTransmission.ACTIVATION, false, false, originatorAddress,
                0, io);

        send(aSdu);
    }

    /**
     * Sends a test command (C_TS_NA_1, TI: 104).
     *
     * @param commonAddress the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @throws IOException if a fatal communication error occurred.
     */
    public void testCommand(int commonAddress) throws IOException {
        ASdu aSdu = new ASdu(ASduType.C_TS_NA_1, false, CauseOfTransmission.ACTIVATION, false, false, originatorAddress,
                commonAddress, new InformationObject(0, new IeFixedTestBitPattern()));

        send(aSdu);
    }

    /**
     * Sends a reset process command (C_RP_NA_1, TI: 105).
     *
     * @param commonAddress the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param qualifier     the qualifier to be sent.
     * @throws IOException if a fatal communication error occurred.
     */
    public void resetProcessCommand(int commonAddress, IeQualifierOfResetProcessCommand qualifier) throws IOException {
        ASdu aSdu = new ASdu(ASduType.C_RP_NA_1, false, CauseOfTransmission.ACTIVATION, false, false, originatorAddress,
                commonAddress, new InformationObject(0, qualifier));
        send(aSdu);
    }

    /**
     * Sends a delay acquisition command (C_CD_NA_1, TI: 106).
     *
     * @param commonAddress the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot           the cause of transmission. Allowed are activation and spontaneous.
     * @param time          the time to be sent.
     * @throws IOException if a fatal communication error occurred.
     */
    public void delayAcquisitionCommand(int commonAddress, CauseOfTransmission cot, IeTime16 time) throws IOException {
        ASdu aSdu = new ASdu(ASduType.C_CD_NA_1, false, cot, false, false, originatorAddress, commonAddress,
                new InformationObject(0, time));
        send(aSdu);
    }

    /**
     * Sends a test command with time tag CP56Time2a (C_TS_TA_1, TI: 107).
     *
     * @param commonAddress       the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param testSequenceCounter the value to be sent.
     * @param time                the time to be sent.
     * @throws IOException if a fatal communication error occurred.
     */
    public void testCommandWithTimeTag(int commonAddress, IeTestSequenceCounter testSequenceCounter, IeTime56 time)
            throws IOException {
        ASdu aSdu = new ASdu(ASduType.C_TS_TA_1, false, CauseOfTransmission.ACTIVATION, false, false, originatorAddress,
                commonAddress, new InformationObject(0, testSequenceCounter, time));
        send(aSdu);
    }

    /**
     * Sends a parameter of measured values, normalized value (P_ME_NA_1, TI: 110).
     *
     * @param commonAddress            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param informationObjectAddress the information object address.
     * @param normalizedValue          the value to be sent.
     * @param qualifier                the qualifier to be sent.
     * @throws IOException if a fatal communication error occurred.
     */
    public void parameterNormalizedValueCommand(int commonAddress, int informationObjectAddress,
                                                IeNormalizedValue normalizedValue, IeQualifierOfParameterOfMeasuredValues qualifier) throws IOException {
        ASdu aSdu = new ASdu(ASduType.P_ME_NA_1, false, CauseOfTransmission.ACTIVATION, false, false, originatorAddress,
                commonAddress, new InformationObject(informationObjectAddress, normalizedValue, qualifier));
        send(aSdu);
    }

    /**
     * Sends a parameter of measured values, scaled value (P_ME_NB_1, TI: 111).
     *
     * @param commonAddress            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param informationObjectAddress the information object address.
     * @param scaledValue              the value to be sent.
     * @param qualifier                the qualifier to be sent.
     * @throws IOException if a fatal communication error occurred.
     */
    public void parameterScaledValueCommand(int commonAddress, int informationObjectAddress, IeScaledValue scaledValue,
                                            IeQualifierOfParameterOfMeasuredValues qualifier) throws IOException {
        ASdu aSdu = new ASdu(ASduType.P_ME_NB_1, false, CauseOfTransmission.ACTIVATION, false, false, originatorAddress,
                commonAddress, new InformationObject(informationObjectAddress, scaledValue, qualifier));
        send(aSdu);
    }

    /**
     * Sends a parameter of measured values, short floating point number (P_ME_NC_1, TI: 112).
     *
     * @param commonAddress            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param informationObjectAddress the information object address.
     * @param shortFloat               the value to be sent.
     * @param qualifier                the qualifier to be sent.
     * @throws IOException if a fatal communication error occurred.
     */
    public void parameterShortFloatCommand(int commonAddress, int informationObjectAddress, IeShortFloat shortFloat,
                                           IeQualifierOfParameterOfMeasuredValues qualifier) throws IOException {
        ASdu aSdu = new ASdu(ASduType.P_ME_NC_1, false, CauseOfTransmission.ACTIVATION, false, false, originatorAddress,
                commonAddress, new InformationObject(informationObjectAddress, shortFloat, qualifier));
        send(aSdu);
    }

    /**
     * Sends a parameter activation (P_AC_NA_1, TI: 113).
     *
     * @param commonAddress            the Common ASDU Address. Valid value are 1...255 or 1...65535 for field lengths 1 or 2 respectively.
     * @param cot                      the cause of transmission. Allowed are activation and deactivation.
     * @param informationObjectAddress the information object address.
     * @param qualifier                the qualifier to be sent.
     * @throws IOException if a fatal communication error occurred.
     */
    public void parameterActivation(int commonAddress, CauseOfTransmission cot, int informationObjectAddress,
                                    IeQualifierOfParameterActivation qualifier) throws IOException {
        ASdu aSdu = new ASdu(ASduType.P_AC_NA_1, false, cot, false, false, originatorAddress, commonAddress,
                new InformationObject(informationObjectAddress, qualifier));
        send(aSdu);
    }

    public void fileReady(int commonAddress, int informationObjectAddress, IeNameOfFile nameOfFile,
                          IeLengthOfFileOrSection lengthOfFile, IeFileReadyQualifier qualifier) throws IOException {
        ASdu aSdu = new ASdu(ASduType.F_FR_NA_1, false, CauseOfTransmission.FILE_TRANSFER, false, false,
                originatorAddress, commonAddress,
                new InformationObject(informationObjectAddress, nameOfFile, lengthOfFile, qualifier));
        send(aSdu);
    }

    public void sectionReady(int commonAddress, int informationObjectAddress, IeNameOfFile nameOfFile,
                             IeNameOfSection nameOfSection, IeLengthOfFileOrSection lengthOfSection, IeSectionReadyQualifier qualifier)
            throws IOException {
        ASdu aSdu = new ASdu(ASduType.F_SR_NA_1, false, CauseOfTransmission.FILE_TRANSFER, false, false,
                originatorAddress, commonAddress,
                new InformationObject(informationObjectAddress, nameOfFile, nameOfSection, lengthOfSection, qualifier));
        send(aSdu);
    }

    public void callOrSelectFiles(int commonAddress, CauseOfTransmission cot, int informationObjectAddress,
                                  IeNameOfFile nameOfFile, IeNameOfSection nameOfSection, IeSelectAndCallQualifier qualifier)
            throws IOException {
        ASdu aSdu = new ASdu(ASduType.F_SC_NA_1, false, cot, false, false, originatorAddress, commonAddress,
                new InformationObject(informationObjectAddress, nameOfFile, nameOfSection, qualifier));
        send(aSdu);
    }

    public void lastSectionOrSegment(int commonAddress, int informationObjectAddress, IeNameOfFile nameOfFile,
                                     IeNameOfSection nameOfSection, IeLastSectionOrSegmentQualifier qualifier, IeChecksum checksum)
            throws IOException {
        ASdu aSdu = new ASdu(ASduType.F_LS_NA_1, false, CauseOfTransmission.FILE_TRANSFER, false, false,
                originatorAddress, commonAddress,
                new InformationObject(informationObjectAddress, nameOfFile, nameOfSection, qualifier, checksum));
        send(aSdu);
    }

    public void ackFileOrSection(int commonAddress, int informationObjectAddress, IeNameOfFile nameOfFile,
                                 IeNameOfSection nameOfSection, IeAckFileOrSectionQualifier qualifier) throws IOException {
        ASdu aSdu = new ASdu(ASduType.F_AF_NA_1, false, CauseOfTransmission.FILE_TRANSFER, false, false,
                originatorAddress, commonAddress,
                new InformationObject(informationObjectAddress, nameOfFile, nameOfSection, qualifier));
        send(aSdu);
    }

    public void sendSegment(int commonAddress, int informationObjectAddress, IeNameOfFile nameOfFile,
                            IeNameOfSection nameOfSection, IeFileSegment segment) throws IOException {
        ASdu aSdu = new ASdu(ASduType.F_SG_NA_1, false, CauseOfTransmission.FILE_TRANSFER, false, false,
                originatorAddress, commonAddress,
                new InformationObject(informationObjectAddress, nameOfFile, nameOfSection, segment));
        send(aSdu);
    }

    public void sendDirectory(int commonAddress, int informationObjectAddress, InformationElement[][] directory)
            throws IOException {
        ASdu aSdu = new ASdu(ASduType.F_DR_TA_1, false, CauseOfTransmission.FILE_TRANSFER, false, false,
                originatorAddress, commonAddress, new InformationObject(informationObjectAddress, directory));
        send(aSdu);
    }

    public void queryLog(int commonAddress, int informationObjectAddress, IeNameOfFile nameOfFile,
                         IeTime56 rangeStartTime, IeTime56 rangeEndTime) throws IOException {
        ASdu aSdu = new ASdu(ASduType.F_SC_NB_1, false, CauseOfTransmission.FILE_TRANSFER, false, false,
                originatorAddress, commonAddress,
                new InformationObject(informationObjectAddress, nameOfFile, rangeStartTime, rangeEndTime));
        send(aSdu);
    }

    public IOException getClosedIOException() {
        return closedIOException;
    }

    /**
     * Time-out of send or test APDUs (t1: default 15 s)
     */
    private class MaxTimeNoAckReceivedTimer extends TimeoutTask {

        public MaxTimeNoAckReceivedTimer() {
            super(SerialConnection.this.settings.getMaxTimeNoAckReceived());
        }

        @Override
        public void execute() {

            synchronized (SerialConnection.this) {
                if (Thread.interrupted()) {
                    return;
                }
                close();
                if (aSduListener != null) {
                    aSduListener.connectionClosed(SerialConnection.this, new IOException(
                            "The maximum time that no confirmation was received (t1) has been exceeded. t1 = "
                                    + settings.getMaxTimeNoAckReceived() + "ms"));
                }
            }
        }
    }

    /**
     * Time-out for acknowledges in case of no data messages t2 < t1 (t2: default 10 s)
     */
    private class MaxTimeNoAckSentTimer extends TimeoutTask {

        public MaxTimeNoAckSentTimer() {
            super(settings.getMaxTimeNoAckSent());
        }

        @Override
        public void execute() {

            synchronized (SerialConnection.this) {
                if (Thread.interrupted()) {
                    return;
                }
                try {
                    sendSFormatPdu();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Time-out for sending test frames in case of a long idle state (t3: default 20 s)
     */
    private class MaxIdleTimeTimer extends TimeoutTask {
        public MaxIdleTimeTimer() {
            super(SerialConnection.this.settings.getMaxIdleTime());
        }

        @Override
        public void execute() {

            synchronized (SerialConnection.this) {
                if (Thread.interrupted()) {
                    return;
                }
                try {
                    os.write(TESTFR_ACT_BUFFER, 0, TESTFR_ACT_BUFFER.length);
                    os.flush();
                } catch (IOException ignore) {
                }
                timeoutManager.addTimerTask(maxTimeNoTestConReceived);
            }
        }

    }

    private class StopDtOutstandingSFormatTimer extends TimeoutTask {

        public StopDtOutstandingSFormatTimer(long timeout) {
            super(timeout);
        }

        @Override
        public void execute() {
            if (Thread.interrupted() || isStopped()) {
                return;
            }
            synchronized (SerialConnection.this) {
                try {
                    sendStopDtCon();
                } catch (IOException ignored) {
                }
            }

        }
    }

    private class ConnectionReader extends Thread {

        @Override
        public void run() {
            Thread.currentThread().setName("ConnectionReader");

            try {
                while (true) {
                    final APdu aPdu = APdu.decode(is, settings);
                    synchronized (SerialConnection.this) {

                        switch (aPdu.getApciType()) {
                            case I_FORMAT:
                                closeIfStopped(aPdu.getApciType());
                                handleIFrame(aPdu);
                                break;
                            case S_FORMAT:
                                closeIfStopped(aPdu.getApciType());
                                handleReceiveSequenceNumber(aPdu.getReceiveSeqNumber());
                                if (stopDtOutstandingSFormatTimer != null) {
                                    stopDtOutstandingSFormatTimer.executeManually();
                                }
                                break;
                            case STARTDT_CON:
                                if (startDtConSignal != null) {
                                    startDtConSignal.countDown();
                                }
                                break;
                            case STARTDT_ACT:
                                handleStartDtAct();
                                if (startDtActSignal != null) {
                                    startDtActSignal.countDown();
                                }
                                break;
                            case TESTFR_ACT:
                                sendTestFrameCon();
                                break;
                            case TESTFR_CON:
                                maxTimeNoTestConReceived.cancel();
                                break;
                            case STOPDT_CON:
                                if (stopDtConSignal != null) {
                                    stopDtConSignal.countDown();
                                }
                                break;
                            case STOPDT_ACT:
                                handleStopDtAct();
                                break;
                            default:
                                // should not occur.
                                throw new IOException("Got unexpected message with APCI Type: " + aPdu.getApciType());
                        }
                        resetMaxIdleTimeTimer();
                    }
                }
            } catch (EOFException e) {
                closedIOException = new EOFException("Connection was closed by remote.");
            } catch (IOException e) {
                closedIOException = e;
            } catch (Exception e) {
                closedIOException = new IOException("Unexpected Exception.", e);
            } finally {
                synchronized (SerialConnection.this) {
                    if (!closed) {
                        close();
                    }
                    if (aSduListener != null) {
                        aSduListener.connectionClosed(SerialConnection.this, closedIOException);
                    }
                    if (stopped && aSduListenerBack != null) {
                        aSduListenerBack.connectionClosed(SerialConnection.this, closedIOException);
                    }
                }
            }
        }

    }
}
