package me.zort.setuplib.exception;

public class NotSetupException extends RuntimeException {

    private final Class<?> clazz;

    public NotSetupException(Class<?> clazz) {
        super("Provided class is not a setup!");
        this.clazz = clazz;
    }

    public Class<?> getProvided() {
        return clazz;
    }

}
