package org.mamotec.common;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class Logger {

	public static void log(String message) {
		String time = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS ").format(new Date());
		System.out.println(time + message);
	}
}
