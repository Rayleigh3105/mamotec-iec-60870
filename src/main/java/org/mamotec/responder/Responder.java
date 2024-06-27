package org.mamotec.responder;

import org.mamotec.common.Logger;
import org.mamotec.common.cli.CliParameterBuilder;
import org.mamotec.common.cli.CliParser;
import org.mamotec.common.cli.IntCliParameter;
import org.mamotec.common.cli.StringCliParameter;
import org.mamotec.responder.client.IecClient;
import org.mamotec.responder.reporter.IecReporter;
import org.mamotec.responder.utils.ActivePowerUtils;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A client/master application to access IEC 60870-5-104 servers/slaves.
 */
public final class Responder {

	private static final StringCliParameter hostParam = new CliParameterBuilder("-h").setMandatory().buildStringParameter("host");

	private static final IntCliParameter portParam = new CliParameterBuilder("-p").buildIntParameter("port", 2404);

	private static final IntCliParameter commonAddrParam = new CliParameterBuilder("-ca").buildIntParameter("common_address", 1);

	private static final IntCliParameter startDtRetries = new CliParameterBuilder("-r").buildIntParameter("start_DT_retries", 1);

	private static final IntCliParameter connectionTimeout = new CliParameterBuilder("-ct").buildIntParameter("connection_timeout", 20_000);

	private static final IntCliParameter messageFragmentTimeout = new CliParameterBuilder("-mft").buildIntParameter("message_fragment_timeout", 5_000);

	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	public static void main(String[] args) throws IOException {
		logStartOfResponder();
		CliParser cliParser = new CliParser("j60870-console-client", "A client/master application to access IEC 60870-5-104 servers/slaves.");
		cliParser.addParameters(hostParam, portParam, commonAddrParam);

		IecClient iecClient = new IecClient(cliParser, args, hostParam, portParam, commonAddrParam, startDtRetries, connectionTimeout, messageFragmentTimeout);
		IecReporter iecReporter = new IecReporter(iecClient);
		iecClient.spinUpClient();

		scheduler.scheduleAtFixedRate(iecReporter::doReport, 0, 5, TimeUnit.SECONDS);

		// Sent active Power
		iecClient.getConnection().send(ActivePowerUtils.buildActivePowerAsdu(11f, commonAddrParam.getValue(), iecClient.getConnection()));
	}

	private static void logStartOfResponder() {
		Logger.log("### Starting Responder ###\n" + "\nHost Address: " + hostParam.getValue() + "\nPort:         " + portParam.getValue());
	}
}