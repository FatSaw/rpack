package me.bomb.rpack.util;

public final class LoggerUtil {
	
	private LoggerUtil() {
	}
	
	private static Logger logger;
	
	public static void setLogger(Logger logger) {
		LoggerUtil.logger = logger;
	}

	public static void info(String msg) {
		if(LoggerUtil.logger == null) {
			return;
		}
		LoggerUtil.logger.info(msg);
	}

	public static void warn(String msg) {
		if(LoggerUtil.logger == null) {
			return;
		}
		LoggerUtil.logger.warn(msg);
	}

	public static void error(String msg) {
		if(LoggerUtil.logger == null) {
			return;
		}
		LoggerUtil.logger.error(msg);
	}

}
