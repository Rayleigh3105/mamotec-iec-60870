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

import org.mamotec.j60870.ReservedASduTypeDecoder;

public class SerialConnectionSettings {

	static {
	}

    private String portName;
    private int baudRate;
    private boolean useSharedThreadPool;
    private int maxUnconfirmedIPdusReceived;
    private int maxNumOfOutstandingIPdus;

    private int maxTimeNoAckReceived;
    private int maxTimeNoAckSent;

    private int cotFieldLength;
    private int commonAddressFieldLength;
    private int ioaFieldLength;

    private int maxIdleTime;
    private ReservedASduTypeDecoder reservedASduTypeDecoder;

    private int messageFragmentTimeout;


    public SerialConnectionSettings() {
        this.portName = "/dev/tty0";
        this.baudRate = 19200;
        this.useSharedThreadPool = false;
        this.maxUnconfirmedIPdusReceived = 8;
        this.maxNumOfOutstandingIPdus = 12;
        this.maxTimeNoAckReceived = 10_000;
        this.maxTimeNoAckSent = 10_000;

        this.cotFieldLength = 2;
        this.commonAddressFieldLength = 2;
        this.ioaFieldLength = 3;
        this.maxIdleTime = 20_000;

        this.messageFragmentTimeout = 5_000;

    }

    public SerialConnectionSettings(SerialConnectionSettings tcpConnectionSettings) {
        portName = tcpConnectionSettings.portName;
        this.useSharedThreadPool = tcpConnectionSettings.useSharedThreadPool;

        baudRate = tcpConnectionSettings.baudRate;
        maxUnconfirmedIPdusReceived = tcpConnectionSettings.maxUnconfirmedIPdusReceived;
        maxNumOfOutstandingIPdus = tcpConnectionSettings.maxNumOfOutstandingIPdus;
        maxTimeNoAckReceived = tcpConnectionSettings.maxTimeNoAckReceived;
        maxTimeNoAckSent = tcpConnectionSettings.maxTimeNoAckSent;

        cotFieldLength = tcpConnectionSettings.cotFieldLength;
        commonAddressFieldLength = tcpConnectionSettings.commonAddressFieldLength;
        ioaFieldLength = tcpConnectionSettings.ioaFieldLength;
        maxIdleTime = tcpConnectionSettings.maxIdleTime;
        reservedASduTypeDecoder = tcpConnectionSettings.reservedASduTypeDecoder;

        messageFragmentTimeout = tcpConnectionSettings.messageFragmentTimeout;

    }

    public String getPortName() {
        return portName;
    }

    public void setPortName(String portName) {
        this.portName = portName;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public void setBaudRate(int baudRate) {
        this.baudRate = baudRate;
    }

    public boolean useSharedThreadPool() {
        return useSharedThreadPool;
    }

    public void setUseSharedThreadPool(boolean useSharedThreadPool) {
        this.useSharedThreadPool = useSharedThreadPool;
    }

    public int getMaxUnconfirmedIPdusReceived() {
        return maxUnconfirmedIPdusReceived;
    }

    public void setMaxUnconfirmedIPdusReceived(int maxUnconfirmedIPdusReceived) {
        this.maxUnconfirmedIPdusReceived = maxUnconfirmedIPdusReceived;
    }

    public int getMaxNumOfOutstandingIPdus() {
        return this.maxNumOfOutstandingIPdus;
    }

    public void setMaxNumOfOutstandingIPdus(int maxNumOfOutstandingIPdus) {
        this.maxNumOfOutstandingIPdus = maxNumOfOutstandingIPdus;
    }

    public int getMaxTimeNoAckReceived() {
        return maxTimeNoAckReceived;
    }

    public void setMaxTimeNoAckReceived(int maxTimeNoAckReceived) {
        this.maxTimeNoAckReceived = maxTimeNoAckReceived;
    }

    public int getMaxTimeNoAckSent() {
        return maxTimeNoAckSent;
    }

    public void setMaxTimeNoAckSent(int maxTimeNoAckSent) {
        this.maxTimeNoAckSent = maxTimeNoAckSent;
    }

    public int getCotFieldLength() {
        return cotFieldLength;
    }

    public void setCotFieldLength(int cotFieldLength) {
        this.cotFieldLength = cotFieldLength;
    }

    public int getCommonAddressFieldLength() {
        return commonAddressFieldLength;
    }

    public void setCommonAddressFieldLength(int commonAddressFieldLength) {
        this.commonAddressFieldLength = commonAddressFieldLength;
    }

    public int getIoaFieldLength() {
        return ioaFieldLength;
    }

    public void setIoaFieldLength(int ioaFieldLength) {
        this.ioaFieldLength = ioaFieldLength;
    }

    public ReservedASduTypeDecoder getReservedASduTypeDecoder() {
        return reservedASduTypeDecoder;
    }

    public void setReservedASduTypeDecoder(ReservedASduTypeDecoder reservedASduTypeDecoder) {
        this.reservedASduTypeDecoder = reservedASduTypeDecoder;
    }

    public int getMaxIdleTime() {
        return maxIdleTime;
    }

    public void setMaxIdleTime(int maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

    public int getMessageFragmentTimeout() {
        return messageFragmentTimeout;
    }

    public void setMessageFragmentTimeout(int messageFragmentTimeout) {
        this.messageFragmentTimeout = messageFragmentTimeout;
    }


}
