package org.mamotec.responder.client;

import lombok.extern.slf4j.Slf4j;
import org.mamotec.common.enums.NeTable;
import org.mamotec.common.type.ValueHolder;
import org.mamotec.j60870.ASdu;
import org.mamotec.j60870.ASduType;
import org.mamotec.j60870.CauseOfTransmission;
import org.mamotec.j60870.ie.IeQuality;
import org.mamotec.j60870.ie.IeShortFloat;
import org.mamotec.j60870.ie.IeTime56;
import org.mamotec.j60870.ie.InformationElement;
import org.mamotec.j60870.ie.InformationObject;
import org.mamotec.j60870.serial.SerialConnection;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class IecClient {

	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	private static final int START_DT_RETRIES = 1;

	private SerialConnection connection = null;

	public IecClient(SerialConnection connection) {
		this.connection = connection;
	}

	private void synchronizeClocks() {
		// Synchronize clocks every hour
		Runnable hourlyTask = () -> {
			System.out.println("Syncronizing clocks...");
			try {
				connection.synchronizeClocks(new IeTime56(System.currentTimeMillis()));
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
			connection.send(new ASdu(ASduType.M_ME_TF_1, false, CauseOfTransmission.SPONTANEOUS, false, false, 0, 1, new InformationObject(neTable.getRegister(),
					new InformationElement[][] {
							{ new IeShortFloat((Float) valueHolder.getValue()), new IeQuality(false, false, false, false, false), new IeTime56(System.currentTimeMillis()) } })));
			return true;
		default:
			log.info("Unsupported ASDU type: {}", neTable.getAsduType());
		}

		return false;
	}

}
