package me.zort.setuplib.exception;

import lombok.Getter;
import me.zort.setuplib.SetupLib;

public class SetupException extends Exception {

    @Getter
    private final SetupLib<?> setup;

    public SetupException(SetupLib<?> setup, String message) {
        super(message);
        this.setup = setup;
    }

    public SetupException(SetupLib<?> setup, Throwable cause, String message) {
        super(message, cause);
        this.setup = setup;
    }

}
