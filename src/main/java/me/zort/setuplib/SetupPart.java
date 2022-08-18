package me.zort.setuplib;

import lombok.Getter;
import me.zort.setuplib.annotation.Setup;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

@Getter
public class SetupPart<T> {

    private final SetupLib<T> setup;
    private final Field field;

    protected SetupPart(SetupLib<T> setup, String fieldName) {
        this.setup = setup;
        try {
            this.field = setup.getTarget().getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public void cancel() {
        setup.cancel();
    }

    public void set(Object obj) {
        setup.getCache().put(field.getName(), obj);
    }

    public void send(Player player) {
        Setup annot = field.getAnnotation(Setup.class);
        String[] lines = annot.message();
        for(SetupLib.SetupMessageDecorator<T> d : setup.getDecorators()) {
            lines = d.modify(this, lines);
        }
        for(String line : lines) {
            setup.send(player, line);
        }
    }

    @Nullable
    public RequiredType getType() {
        return RequiredType.valueOf(field.getType());
    }

    public Setup getAnnot() {
        return field.getAnnotation(Setup.class);
    }

    public String getName() {
        return field.getName();
    }

}
