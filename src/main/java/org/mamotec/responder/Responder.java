package org.mamotec.responder;

import org.mamotec.common.cli.CliParameterBuilder;
import org.mamotec.common.cli.CliParseException;
import org.mamotec.common.cli.CliParser;
import org.mamotec.common.cli.IntCliParameter;
import org.mamotec.common.cli.StringCliParameter;
import org.mamotec.common.enums.NeTable;
import org.mamotec.common.type.ValueHolder;
import org.mamotec.j60870.ClientConnectionBuilder;
import org.mamotec.j60870.Connection;
import org.mamotec.j60870.serial.SerialConnection;
import org.mamotec.j60870.serial.SerialConnectionImpl;
import org.mamotec.j60870.serial.SerialConnectionSettings;
import org.mamotec.j60870.serial.SerialServer;
import org.mamotec.responder.client.IecClient;
import org.mamotec.responder.client.IecClientEventListenerSerial;
import org.mamotec.responder.client.IecClientEventListenerTcp;
import org.mamotec.responder.reporter.IecReporter;
import org.mamotec.responder.server.IecServer;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.mamotec.common.Logger.log;

/**
 * A client/master application to access IEC 60870-5-104 servers/slaves.
 */
public final class Responder {

	private static final StringCliParameter hostParam = new CliParameterBuilder("-h").buildStringParameter("host");

	private static final StringCliParameter baudRate = new CliParameterBuilder("-b").setMandatory().buildStringParameter("baudrate");

	private static final StringCliParameter portParam = new CliParameterBuilder("-p").setMandatory().buildStringParameter("port");

	private static final IntCliParameter commonAddrParam = new CliParameterBuilder("-ca").buildIntParameter("common_address", 1);

	private static final IntCliParameter startDtRetries = new CliParameterBuilder("-r").buildIntParameter("start_DT_retries", 1);

	private static final IntCliParameter connectionTimeout = new CliParameterBuilder("-ct").buildIntParameter("connection_timeout", 20_000);

	private static final IntCliParameter messageFragmentTimeout = new CliParameterBuilder("-mft").buildIntParameter("message_fragment_timeout", 5_000);

	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	public static void main(String[] args) throws IOException {
		logStartOfResponder(args);

		SerialConnectionImpl serialConnection = startSerialConnection();
		serialConnection.open();
		serialConnection.start();
		//IecClient iecClient = new IecClient(serialConnection);
		//iecClient.spinUpClient(new IecClientEventListenerSerial());
		// IecReporter iecReporter = new IecReporter(iecClient);


		//scheduler.scheduleAtFixedRate(iecReporter::doReport, 1, 5, java.util.concurrent.TimeUnit.SECONDS);

	}

	private static SerialConnectionImpl startSerialConnection() {
		SerialConnectionSettings serialConnectionSettings = new SerialConnectionSettings();
		serialConnectionSettings.setPortName(portParam.getValue());
		serialConnectionSettings.setBaudRate(Integer.parseInt(baudRate.getValue()));
		try {
			return new SerialConnectionImpl(serialConnectionSettings);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void logStartOfResponder(String[] args) {
		CliParser cliParser = new CliParser("j60870-console-responder", "A client/master application to access IEC 60870-5-104 servers/slaves.");
		cliParser.addParameters(hostParam, portParam, commonAddrParam, baudRate);

		try {
			cliParser.parseArguments(args);
		} catch (CliParseException e1) {
			System.err.println("Error parsing command line parameters: " + e1.getMessage());
			log(cliParser.getUsageString());
			System.exit(1);
		}

		log("### Starting Responder ###\n" + "\nHost Address: " + hostParam.getValue() + "\nPort:         " + portParam.getValue() + "\nBaudrate:         " + baudRate.getValue());
	}
}