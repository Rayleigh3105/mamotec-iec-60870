package org.mamotec.j60870.serial;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SerialConnectionSettings {

	private String portName;

	private int baudRate;

	private int cotFieldLength;

	private int commonAddressFieldLength;

	private int ioaFieldLength;

	private int dataBits;

	private int stopBits;

	private int parity;

	public SerialConnectionSettings() {
		this.portName = "/dev/tty0";
		this.baudRate = 19200;

		this.cotFieldLength = 2;
		this.commonAddressFieldLength = 2;
		this.ioaFieldLength = 3;
		this.dataBits = 8;
		this.stopBits = 1;
		this.parity = 2; // EVEN

	}

}
