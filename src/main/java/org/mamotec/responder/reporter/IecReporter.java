package org.mamotec.responder.reporter;

import lombok.extern.slf4j.Slf4j;
import org.mamotec.common.enums.AccessType;
import org.mamotec.common.enums.NeTable;
import org.mamotec.common.threshhold.Threshold;
import org.mamotec.common.type.ValueHolder;
import org.mamotec.responder.client.IecClient;
import org.mamotec.responder.modbus.ModbusTcpClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.stream;
import static org.mamotec.common.enums.NeTable.values;

@Slf4j
public class IecReporter {

	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	private final IecClient iecGateway;

	private final ModbusTcpClient modbusTcpClient;

	private final int millis;

	public IecReporter(IecClient iecGateway, ModbusTcpClient modbusTcpClient, int intervalInSeconds) {
		this.iecGateway = iecGateway;
		this.modbusTcpClient = modbusTcpClient;
		this.millis = intervalInSeconds;
	}

	public void startReporting() {
		scheduler.scheduleAtFixedRate(this::doReport, 0, millis, TimeUnit.MILLISECONDS);
	}

	public void doReport() {
		ArrayList<NeTable> readRegister = new ArrayList<>(stream(values()).filter(neTable -> neTable.getModbusAccessType().equals(AccessType.READ)).toList());
		log.info("Reading {} tables", readRegister.size());
		try {
			readRegister.forEach(neTable -> {
				// Read table from Modbus Server
				ValueHolder<Object> holder = modbusTcpClient.read(neTable);

				// Save the last measured value
				Object lastValue = neTable.getLastMeasuredValue();
				neTable.setLastMeasuredValue(holder.getValue());

				// Check thresholds
				for (Threshold threshold : neTable.getThresholds()) {
					if (lastValue != null && threshold.check((Number) holder.getValue(), (Number) lastValue)) {
						log.warn("Threshold exceeded for {}: {}", neTable, holder.getValue());
					}
				}

				// Send command via IEC Client
				boolean isSent;
				try {
					isSent = iecGateway.sendValue(holder, neTable);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}

				if (isSent) {
					log.info("Value sent successfully: {}", holder.getValue());
				} else {
					log.info("Failed to send value: {}", holder.getValue());
				}
			});

		} catch (Exception e) {
			log.error("Error in doReport: {}", e.getMessage());
		}

	}
}
