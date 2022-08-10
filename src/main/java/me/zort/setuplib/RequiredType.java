package me.zort.setuplib;

import com.google.common.primitives.Primitives;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public enum RequiredType {

    STRING(String.class, s -> s),
    INTEGER(Integer.class, Integer::parseInt),
    BOOLEAN(Boolean.class, Boolean::parseBoolean),
    DOUBLE(Double.class, Double::parseDouble),
    FLOAT(Float.class, Float::parseFloat),
    LONG(Long.class, Long::parseLong),
    SHORT(Short.class, Short::parseShort),
    BYTE(Byte.class, Byte::parseByte),
    CHARACTER(Character.class, c -> c.charAt(0));

    public static RequiredType valueOf(Class<?> wrapperType) {
        wrapperType = Primitives.wrap(wrapperType);

        // This is so annoying.
        final Class<?> wtFinal = wrapperType;
        return Arrays.stream(values())
                .filter(type -> type.getWrapperType().equals(wtFinal))
                .findFirst().orElse(null);
    }

    @Getter
    private final Class<?> wrapperType;
    private final TypeParser parser;

    RequiredType(Class<?> wrapperType, TypeParser parser) {
        this.wrapperType = wrapperType;
        this.parser = parser;
    }

    public Object parse(String arg) {
        try {
            return parser.parse(arg);
        } catch(Exception e) {
            return null;
        }
    }

    public boolean canParse(String arg) {
        return parse(arg) != null;
    }

    public interface TypeParser {

        @Nullable
        Object parse(String arg);

    }

}
