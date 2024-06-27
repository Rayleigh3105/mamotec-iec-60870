package org.mamotec.responder.utils;

import org.mamotec.j60870.ASdu;
import org.mamotec.j60870.CauseOfTransmission;
import org.mamotec.j60870.Connection;
import org.mamotec.j60870.ie.IeShortFloat;
import org.mamotec.j60870.ie.InformationElement;
import org.mamotec.j60870.ie.InformationObject;

import static org.mamotec.j60870.ASduType.M_ME_TF_1;

public class ActivePowerUtils {

	public static ASdu buildActivePowerAsdu(Float activePower, int commonAddress, Connection connection) {
		InformationObject obj = new InformationObject(43, new InformationElement[][] { { new IeShortFloat(activePower) } });
		return new ASdu(M_ME_TF_1, true, CauseOfTransmission.SPONTANEOUS, true, false, connection.getOriginatorAddress(), commonAddress, obj);
	}
}
