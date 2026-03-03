package com.openmind.hagap.domain.exception;

public class CliExecutionException extends RuntimeException {

    public CliExecutionException(String message) {
        super(message);
    }

    public CliExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
