package org.mamotec.responder.server;

import org.mamotec.common.cli.CliParameterBuilder;
import org.mamotec.common.cli.IntCliParameter;
import org.mamotec.common.cli.StringCliParameter;
import org.mamotec.j60870.tcp.TcpServer;

import java.io.IOException;
import java.net.InetAddress;

import static org.mamotec.common.Logger.log;

public class IecServer {

	private final StringCliParameter host;

	private final IntCliParameter port;

	private static final IntCliParameter caLengthParam = new CliParameterBuilder("-cal").setDescription("Common Address (CA) field length.").buildIntParameter("ca_length", 2);

	private static final IntCliParameter iaoLengthParam = new CliParameterBuilder("-iaol").setDescription("Information Object Address (IOA) field length.")
			.buildIntParameter("iao_length", 3);

	private static final IntCliParameter cotLengthParam = new CliParameterBuilder("-cotl").setDescription("Cause Of Transmission (CoT) field length.")
			.buildIntParameter("cot_length", 2);

	public IecServer(StringCliParameter hostParam, IntCliParameter portParam) {
		this.host = hostParam;
		this.port = portParam;
	}

	public void spinUpServer() {
		try {
			log("### Starting Server ###\n", "\nBind Address: ", host.getValue(), "\nPort:         ", String.valueOf(port.getValue()), "\nIAO length:   ",
					String.valueOf(iaoLengthParam.getValue()), "\nCA length:    ", String.valueOf(caLengthParam.getValue()), "\nCOT length:   ",
					String.valueOf(cotLengthParam.getValue()), "\n");

			TcpServer.Builder builder = TcpServer.builder();
			InetAddress bindAddress = InetAddress.getByName(host.getValue());
			builder.setBindAddr(bindAddress).setPort(port.getValue()).setIoaFieldLength(iaoLengthParam.getValue()).setCommonAddressFieldLength(caLengthParam.getValue())
					.setCotFieldLength(cotLengthParam.getValue());
			TcpServer tcpServer = builder.build();

			tcpServer.start(new IecTcpServerListener());
		} catch (IOException e) {
			log("Unable to start listening: \"", e.getMessage(), "\". Will quit.");
		}
	}
}
