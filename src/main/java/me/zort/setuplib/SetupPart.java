package me.zort.setuplib;

import lombok.Getter;
import me.zort.setuplib.annotation.Setup;
import org.apache.commons.lang.ArrayUtils;
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
        String[] oldLines = annot.message();
        String[] lines = new String[0];
        for(String line : oldLines) {
            if(line.startsWith("{") && line.endsWith("}") && line.length() > 2) {
                lines = (String[]) ArrayUtils.addAll(lines,
                        setup.getPlaceholderMessageBuilder()
                                .build(line.substring(1, line.length() - 1))
                                .toArray(new String[0])
                );
            } else {
                lines = (String[]) ArrayUtils.add(lines, line);
            }
        }
        for(SetupLib.SetupMessageDecorator<T> d : setup.getDecorators()) {
            lines = d.modify(this, lines);
        }
        for(String line : lines) {
            setup.getMessageSender().accept(player, line);
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
