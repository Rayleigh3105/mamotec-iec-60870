package org.mamotec.responder.utils;

import org.mamotec.j60870.ASdu;
import org.mamotec.j60870.CauseOfTransmission;
import org.mamotec.j60870.Connection;
import org.mamotec.j60870.tcp.TcpConnection;
import org.mamotec.j60870.ie.IeShortFloat;
import org.mamotec.j60870.ie.IeTime24;
import org.mamotec.j60870.ie.InformationElement;
import org.mamotec.j60870.ie.InformationObject;

import static org.mamotec.j60870.ASduType.M_ME_TF_1;

public class ActivePowerUtils {

	public static ASdu buildActivePowerAsdu(Integer activePower, int commonAddress, Connection tcpConnection) {
		InformationElement[] informationElements = new InformationElement[] {
				new IeShortFloat(activePower.floatValue()),
				new IeTime24(System.currentTimeMillis())
		};

		InformationObject obj = new InformationObject(1, informationElements);
		return new ASdu(M_ME_TF_1, false, CauseOfTransmission.SPONTANEOUS, false, false, 0, 1, obj);
	}
}
