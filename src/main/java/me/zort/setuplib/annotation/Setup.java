package me.zort.setuplib.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents a setup part field
 * in mapping and target class.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Setup {

    /**
     * Message that shows up on start of this
     * setup part.
     *
     * @return The message.
     */
    String[] message();

    /**
     * Message that shows up when player inputs wrong
     * format in the chat.
     *
     * @return The message.
     */
    String[] invalidFormat() default {};

}
