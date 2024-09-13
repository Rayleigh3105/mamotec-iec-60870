package org.mamotec.j60870.serial;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import lombok.extern.slf4j.Slf4j;
import org.mamotec.Responder;
import org.mamotec.j60870.ASdu;
import org.mamotec.j60870.internal.ExtendedDataInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class SerialPortDataListenerImpl implements SerialPortDataListener {

	private final SerialConnectionSettings settings;

	public SerialPortDataListenerImpl(SerialConnectionSettings settings) {
		this.settings = settings;
	}

	@Override
	public int getListeningEvents() {
		return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
	}

	@Override
	public void serialEvent(SerialPortEvent event) {

		if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_RECEIVED) {
			return;
		}

        byte[] newData = event.getReceivedData();

		InputStream inputStream = new ByteArrayInputStream(newData);

		ExtendedDataInputStream extendedDataInputStream = new ExtendedDataInputStream(inputStream);

		try {
			log.info("Received data: {} bytes", newData.length);
			// Lese das Startbyte
			int startByte = extendedDataInputStream.readUnsignedByte();

			if (startByte == 0x68) {
				ASdu.decode(extendedDataInputStream, settings, 3);
			} else if (startByte == 0x10) {
				processSingleByteFrame(extendedDataInputStream, event);
			} else {
				// Unbekanntes Startbyte
				log.info("Unbekanntes Startbyte: {}", String.format("0x%02X", startByte));
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	private void processSingleByteFrame(ExtendedDataInputStream inputStream, SerialPortEvent event) throws IOException {
		// Lese das Kontrollfeld
		int controlField = inputStream.readUnsignedByte();

		// Verarbeitung des Kontrollfeldes, um den Typ des Einzelbyte-Frames zu bestimmen
		switch (controlField) {
		case 0x49:
			log.info("Empfangen: Request for Link Status");
			event.getSerialPort().getOutputStream().write(SerialConnection.STATUS);
			event.getSerialPort().getOutputStream().flush();
			break;
		case 0x40:
			log.info("Empfangen: Request of remote link");
			event.getSerialPort().getOutputStream().write(SerialConnection.ACK);
			event.getSerialPort().getOutputStream().flush();
			break;

		default:
			log.info("Unbekanntes Kontrollfeld: {}", String.format("0x%02X", controlField));
			break;
		}
	}
}
