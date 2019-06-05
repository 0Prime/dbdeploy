package com.dbdeploy.exceptions;

import java.text.MessageFormat;

public class UsageException extends DbDeployException {

	public UsageException(String message) {
		super(message);
	}

	public UsageException(String message, Throwable throwable) {
		super(message, throwable);
	}

	public static UsageException of(String format, Object... args) {
		return new UsageException(MessageFormat.format(format, args));
	}

	public static void throwForMissingRequiredValue(String valueName) throws UsageException {
		throw new UsageException(valueName + " required");
	}
}
