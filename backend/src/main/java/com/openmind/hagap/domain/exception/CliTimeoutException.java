package com.openmind.hagap.domain.exception;

public class CliTimeoutException extends CliExecutionException {

    public CliTimeoutException(int timeoutSeconds) {
        super("CLI execution timed out after " + timeoutSeconds + " seconds");
    }
}
