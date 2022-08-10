package me.zort.setuplib.exception;

import lombok.Getter;

import java.util.List;

/**
 * This exception is used for handling unacceptable inputs
 * for custom types.
 * <p>
 * Basically this exception allows you to send player
 * error messages while anything occurs in your own
 * type build.
 * <p>
 * For example: Player inputs nickname of player,
 * but that nickname is his own and he cannot input
 * himself into the args.
 *
 * @author ZorTik
 */
public class InputNotAcceptibleException extends Exception {

    @Getter
    private final List<String> messageLines;

    public InputNotAcceptibleException(List<String> message) {
        this.messageLines = message;
    }

}
