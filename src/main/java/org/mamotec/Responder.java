package org.mamotec;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.mamotec.common.cli.CliParameterBuilder;
import org.mamotec.common.cli.CliParseException;
import org.mamotec.common.cli.CliParser;
import org.mamotec.common.cli.IntCliParameter;
import org.mamotec.common.cli.StringCliParameter;
import org.mamotec.j60870.ASdu;
import org.mamotec.j60870.ASduType;
import org.mamotec.j60870.CauseOfTransmission;
import org.mamotec.j60870.ie.IeQualifierOfSetPointCommand;
import org.mamotec.j60870.ie.IeShortFloat;
import org.mamotec.j60870.ie.IeSingleCommand;
import org.mamotec.j60870.ie.InformationElement;
import org.mamotec.j60870.ie.InformationObject;
import org.mamotec.j60870.serial.SerialConnection;
import org.mamotec.j60870.serial.SerialConnectionSettings;
import org.mamotec.responder.modbus.ModbusTcpClient;
import org.mamotec.thread.IecThread;
import org.mamotec.thread.SerialConnectionReadThread;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * A client/master application to access IEC 60870-5-104 servers/slaves.
 */
@Slf4j
@Getter
public class Responder {

	private static final byte[] STATUS = new byte[] { (byte) 0xE5 };

	// Serial connection parameters
	private static final StringCliParameter baudRate = new CliParameterBuilder("-br").setMandatory().buildStringParameter("baudrate");

	private static final StringCliParameter portParam = new CliParameterBuilder("-po").setMandatory().buildStringParameter("port");

	private static final IntCliParameter dataBits = new CliParameterBuilder("-db").buildIntParameter("data_bits", 8);

	private static final IntCliParameter parity = new CliParameterBuilder("-pa").buildIntParameter("parity", 2);

	private static final IntCliParameter stopBit = new CliParameterBuilder("-sb").buildIntParameter("stop_bit", 1);

	// IEC 60870-5-101 parameters
	private static final IntCliParameter linkAddress = new CliParameterBuilder("-la").buildIntParameter("link_address", 1);

	private static final IntCliParameter ioaSize = new CliParameterBuilder("-is").buildIntParameter("ioa_size", 3);

	private static final IntCliParameter asduAddressSize = new CliParameterBuilder("-ads").buildIntParameter("asdu_address_size", 2);

	private static final IntCliParameter cotSize = new CliParameterBuilder("-cs").buildIntParameter("cot_size", 2);

	// Modbus TCP parameters
	private static final StringCliParameter modbusTcpHost = new CliParameterBuilder("-mod").buildStringParameter("modbus_tcp_host", "0.0.0.0");

	private static SerialConnection serialConnection;

	private static ModbusTcpClient modbusTcpClient;

	public static void main(String[] args) {
		logStartOfResponder(args);

		doPreconditions();

		try {
			ASdu aSdu50 = new ASdu(ASduType.C_SE_NC_1, false, CauseOfTransmission.SPONTANEOUS, false, false, 0, 1,
					new InformationObject(111, new InformationElement[][] { { new IeShortFloat(32F), new IeQualifierOfSetPointCommand(0, true) } }));
			serialConnection.send(aSdu50);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// Thread 1: Serial Connection Read
		SerialConnectionReadThread serialConnectionTask = new SerialConnectionReadThread(serialConnection);
		Thread serialConnectionThread = new Thread(serialConnectionTask);

		// Thread 2: IEC Client and Reporter
		IecThread iecThread = new IecThread(serialConnection, modbusTcpClient);
		Thread iecThreadThread = new Thread(iecThread);
/*
		serialConnectionThread.start();
		iecThreadThread.start();*/
	}

	private static void doPreconditions() {
		// Create Serial connection
		serialConnection = new SerialConnection(createSerialConnectionSettings());
		serialConnection.openPort();

		// Create ModbusTcpConnection
/*		try {
			modbusTcpClient = new ModbusTcpClient(modbusTcpHost.getValue());
			modbusTcpClient.connect();

		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		if (serialConnection.isOpen() && modbusTcpClient.isConnected()) {
			log.info("Preconditions done: Serial connection opened and Modbus TCP client connected.");
		} else {
			if (!serialConnection.isOpen()) {
				log.error("Serial connection not opened.");
			}
			if (!modbusTcpClient.isConnected()) {
				log.error("Modbus TCP client not connected.");
			}
			throw new RuntimeException("Preconditions failed: Serial connection not opened or Modbus TCP client not connected.");
		}*/
	}

	private static SerialConnectionSettings createSerialConnectionSettings() {
		SerialConnectionSettings serialConnectionSettings = new SerialConnectionSettings();
		serialConnectionSettings.setCotFieldLength(cotSize.getValue());
		serialConnectionSettings.setIoaFieldLength(ioaSize.getValue());
		serialConnectionSettings.setCommonAddressFieldLength(asduAddressSize.getValue());
		serialConnectionSettings.setPortName(portParam.getValue());
		serialConnectionSettings.setBaudRate(Integer.parseInt(baudRate.getValue()));
		serialConnectionSettings.setDataBits(dataBits.getValue());
		serialConnectionSettings.setStopBits(stopBit.getValue());
		serialConnectionSettings.setParity(parity.getValue());
		return serialConnectionSettings;
	}

	private static void logStartOfResponder(String[] args) {
		CliParser cliParser = new CliParser("Responder", "A client application to access IEC 60870-5-101 master.");
		cliParser.addParameters(baudRate, portParam, dataBits, parity, stopBit, linkAddress, asduAddressSize, ioaSize, cotSize, modbusTcpHost);

		try {
			cliParser.parseArguments(args);
		} catch (CliParseException e1) {
			log.error("Error parsing command line parameters: {}", e1.getMessage());
			log.info(cliParser.getUsageString());
			System.exit(1);
		}

		//@formatter:off
		log.info("\n\n==============================\n" +
				"      Starting Responder 	   \n" +
				"==============================\n" +
				"Port:           {}\n" +
				"Baudrate:       {}\n" +
				"Data Bits:      {}\n" +
				"Parity:         {}\n" +
				"Stop Bit:       {}\n" +
				"Link Address:   {}\n" +
				"ASDU Addr Size: {}\n" +
				"IOA Size:       {}\n" +
				"COT Size:       {}\n" +
				"==============================\n\n",
				portParam.getValue(),
				baudRate.getValue(),
				dataBits.getValue(),
				parity.getValue(),
				stopBit.getValue(),
				linkAddress.getValue(),
				asduAddressSize.getValue(),
				ioaSize.getValue(),
				cotSize.getValue());
		//@formatter:on
	}

	public void otherStuff() {
		// 30
/*		serialConnection.send(new ASdu(ASduType.M_SP_TB_1, false, CauseOfTransmission.SPONTANEOUS, false, false, 0,
				1,
				new InformationObject(12,
						new InformationElement[][]{{
								new IeSinglePointWithQuality(false, false, false, false, false),
								new IeTime56(System.currentTimeMillis())}})));*/

		// 31
/*
		serialConnection.send(new ASdu(ASduType.M_DP_TB_1, false, CauseOfTransmission.SPONTANEOUS, false, false, 0, 1, new InformationObject(12, new InformationElement[][] {
				{ new IeDoublePointWithQuality(IeDoublePointWithQuality.DoublePointInformation.OFF, false, false, false, false), new IeTime56(System.currentTimeMillis()) } })));
*/

		//serialConnection.synchronizeClocks(1, new IeTime56(System.currentTimeMillis()));

		// 36
/*		serialConnection.send(new ASdu(ASduType.M_ME_TF_1, false, CauseOfTransmission.SPONTANEOUS, false, false, 0,
				1,
				new InformationObject(134,
						new InformationElement[][]{{
								new IeShortFloat(300.8f),
								new IeQuality(false, false, false, false, false),
								new IeTime56(System.currentTimeMillis())}})));*/

		// 45
		IeSingleCommand singleCommand = new IeSingleCommand(true, 1, false);
		ASdu aSdu = new ASdu(ASduType.C_SC_NA_1, false, CauseOfTransmission.ACTIVATION_CON, false, false, 0, 1,
				new InformationObject(0, new InformationElement[][] { { singleCommand } }));
		//serialConnection.send(aSdu);

		// 50
		ASdu aSdu50 = new ASdu(ASduType.C_SE_NC_1, false, CauseOfTransmission.SPONTANEOUS, false, false, 0, 1,
				new InformationObject(111, new InformationElement[][] { { new IeShortFloat(32F), new IeQualifierOfSetPointCommand(0, true) } }));
		//serialConnection.send(aSdu50);
	}
}