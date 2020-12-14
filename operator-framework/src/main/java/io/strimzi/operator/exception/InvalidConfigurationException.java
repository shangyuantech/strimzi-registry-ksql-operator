package io.strimzi.operator.exception;

public class InvalidConfigurationException extends RuntimeException  {

    public InvalidConfigurationException(String message) {
        super(message);
    }

    public InvalidConfigurationException(Throwable cause) {
        super(cause);
    }

    public InvalidConfigurationException(String message, Throwable t) {
        super(message, t);
    }
}
