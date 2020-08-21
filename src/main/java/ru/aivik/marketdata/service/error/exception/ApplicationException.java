package ru.aivik.marketdata.service.error.exception;

public class ApplicationException extends RuntimeException {

    private final Type type;

    public ApplicationException(String message, Type type) {
        super(message);
        this.type = type;
    }

    public ApplicationException(String message, Throwable throwable, Type type) {
        super(message, throwable);
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        Execution, Invocation, System
    }

}
