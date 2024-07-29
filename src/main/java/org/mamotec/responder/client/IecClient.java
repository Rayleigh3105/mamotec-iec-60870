package org.mamotec.responder.client;

import org.mamotec.common.enums.NeTable;
import org.mamotec.common.type.ValueHolder;
import org.mamotec.j60870.ASdu;
import org.mamotec.j60870.ASduType;
import org.mamotec.j60870.CauseOfTransmission;
import org.mamotec.j60870.Connection;
import org.mamotec.j60870.ie.IeShortFloat;
import org.mamotec.j60870.ie.IeSinglePointWithQuality;
import org.mamotec.j60870.ie.IeTime24;
import org.mamotec.j60870.ie.IeTime56;
import org.mamotec.j60870.ie.InformationElement;
import org.mamotec.j60870.ie.InformationObject;
import org.mamotec.responder.utils.ActivePowerUtils;

import java.io.IOException;
import java.util.EventListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.mamotec.j60870.ASduType.M_ME_TF_1;
import static org.mamotec.j60870.ASduType.M_SP_NA_1;

public class IecClient {

	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	private static final int START_DT_RETRIES = 1;

	private Connection connection = null;

	public IecClient(Connection connection) {
		this.connection = connection;
	}

	public void spinUpClient(EventListener eventListener) {
		try {
			connection.open();
			for (int i = 1; i <= START_DT_RETRIES; i++) {
				try {
					connection.startDataTransfer(eventListener);
					break;
				} catch (IOException e) {
					if (i == START_DT_RETRIES) {
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
				connection.synchronizeClocks(0, new IeTime56(System.currentTimeMillis()));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		};
		scheduler.scheduleAtFixedRate(hourlyTask, 1, 5, TimeUnit.SECONDS);
	}

	public <T> boolean sendValue(ValueHolder<T> valueHolder, NeTable neTable) throws IOException {
		switch (neTable.getAsduType()) {
		case M_ME_TF_1:
			// 36 - Measured value, short floating point number with time tag CP56Time2a
			connection.send(ActivePowerUtils.buildActivePowerAsdu((Integer) valueHolder.getValue(), 0, connection));
			break;
		default:
			System.out.println("Unsupported ASDU type: " + neTable.getAsduType());
		}

		return false;
	}

}
