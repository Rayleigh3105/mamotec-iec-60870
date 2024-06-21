package org.mamotec.responder.client;

import org.mamotec.common.cli.CliParser;
import org.mamotec.common.cli.IntCliParameter;
import org.mamotec.common.cli.StringCliParameter;
import org.mamotec.j60870.ClientConnectionBuilder;
import org.mamotec.j60870.Connection;
import org.mamotec.j60870.ie.IeTime56;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Client {

	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	private static Connection connection;

	private final CliParser cliParser;

	private final String[] args;

	private final StringCliParameter host;

	private final IntCliParameter port;

	private final IntCliParameter commonAddr;

	private final IntCliParameter startDtRetries;

	private final IntCliParameter connectionTimeout;

	private final IntCliParameter messageFragmentTimeout;


	public Client(CliParser cliParserParam, String[] argsParam, StringCliParameter hostParam, IntCliParameter portParam, IntCliParameter commonAddrParam,
			IntCliParameter startDtRetries, IntCliParameter connectionTimeout, IntCliParameter messageFragmentTimeout) {
		cliParser = cliParserParam;
		args = argsParam;
		this.host = hostParam;
		this.port = portParam;
		this.commonAddr = commonAddrParam;
		this.startDtRetries = startDtRetries;
		this.connectionTimeout = connectionTimeout;
		this.messageFragmentTimeout = messageFragmentTimeout;


	}

	public void spinUpClient() {
		try {
			cliParser.parseArguments(args);
			InetAddress address = InetAddress.getByName(host.getValue());
			connection = new ClientConnectionBuilder(address).setMessageFragmentTimeout(messageFragmentTimeout.getValue()).setConnectionTimeout(connectionTimeout.getValue())
					.setPort(port.getValue()).build();

			Runtime.getRuntime().addShutdownHook(new Thread(() -> connection.close()));

			for (int i = 1; i <= startDtRetries.getValue(); i++) {
				try {
					connection.startDataTransfer(new ClientEventListener());
					break;
				} catch (IOException e) {
					if (i == startDtRetries.getValue()) {
						connection.close();
						return;
					}
				}
			}

			synchronizeClocks();

		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
	}

	private void synchronizeClocks() {
		// Synchronize clocks every hour
		Runnable hourlyTask = () -> {
			System.out.println("Syncronizing clocks...");
			try {
				connection.synchronizeClocks(commonAddr.getValue(), new IeTime56(System.currentTimeMillis()));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		};
		scheduler.scheduleAtFixedRate(hourlyTask, 1, 1, TimeUnit.SECONDS);
	}
}
