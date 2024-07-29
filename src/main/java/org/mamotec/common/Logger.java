package org.mamotec.common;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class Logger {

	public static void log(String... strings) {
		String time = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS ").format(new Date());
		println(time, strings);
	}

	public static void logPrefix(String prefix, String... strings) {
		String time = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS ").format(new Date());
		printlnPrefix(time, prefix, strings);
	}


	public static void println(String string, String... strings) {
		StringBuilder sb = new StringBuilder();
		sb.append(string);
		for (String s : strings) {
			sb.append(s);
		}
		System.out.println(sb);
	}

	public static void printlnPrefix(String string, String prefix, String... strings) {
		StringBuilder sb = new StringBuilder();
		sb.append(string).append("[").append(prefix).append("] ");
		for (String s : strings) {
			sb.append(s);
		}
		System.out.println(sb);
	}
}
