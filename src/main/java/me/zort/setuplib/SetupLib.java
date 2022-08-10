package me.zort.setuplib;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.primitives.Primitives;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.zort.setuplib.annotation.Setup;
import me.zort.setuplib.exception.InputNotAcceptibleException;
import me.zort.setuplib.exception.NotSetupException;
import me.zort.setuplib.exception.SetupException;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SetupLib<T> implements Iterator<SetupPart<T>>, Cloneable {

    private static final Map<String, SetupLibListener> LISTENERS = Maps.newConcurrentMap();

    public static void init(Plugin plugin) {
        clear(plugin);
        LISTENERS.put(plugin.getName(), new SetupLibListener(plugin));
    }

    public static void clear(Plugin plugin) {
        clear(plugin.getName());
    }

    public static void clear(String pluginName) {
        Optional.ofNullable(LISTENERS.get(pluginName))
                .ifPresent(HandlerList::unregisterAll);
        LISTENERS.remove(pluginName);
    }

    public static <T> SetupLib<T> create(Plugin plugin, Class<T> clazz) {
        return new SetupLib<>(plugin, clazz);
    }

    @Getter(AccessLevel.PROTECTED)
    private final Class<T> target;
    @Getter(AccessLevel.PROTECTED)
    private final Plugin plugin;
    @Getter(AccessLevel.PROTECTED)
    private final Map<String, Object> cache;
    private SetupMessageDecorator<T>[] decorators;
    @Getter(AccessLevel.PROTECTED)
    private Map<Class<?>, CustomTypeBuilder<?>> customTypes;
    private FinishHandler<T> finishHandler;
    private ErrorHandler<T> errorHandler;


    private final List<CompletableFuture<T>> futures;

    @Getter(AccessLevel.PROTECTED)
    private SetupPart<T> current;

    /**
     * Constructs new SetupLib instance.
     *
     * @param plugin The plugin to use.
     * @param target Target of the setup.
     *
     * @throws NotSetupException if target does not contain any {@link Setup}.
     */
    public SetupLib(Plugin plugin, Class<T> target) throws NotSetupException {
        checkSetup(target);
        this.target = target;
        this.plugin = plugin;
        this.cache = new HashMap<>();
        this.current = null;
        this.futures = Collections.synchronizedList(new ArrayList<>());
        this.customTypes = new HashMap<>();
        onFinish((player, result) -> {});
        onError((player, err) -> {});
        setDecorator(null);
    }

    public SetupLib<T> setDecorator(@Nullable SetupMessageDecorator<T> decorator) {
        this.decorators = new SetupMessageDecorator[] {
                (SetupMessageDecorator<T>) (part, message) -> message
        };
        addDecorator(decorator);
        return this;
    }

    /**
     * Adds new decorator to this instance.
     * Decorators are used to modify messages while sending
     * to the player. Developer can use this to reduce amount
     * of duplicate lines in {@link Setup} annotations.
     *
     * @param decorator The decorator.
     * @return This instance.
     */
    public SetupLib<T> addDecorator(SetupMessageDecorator<T> decorator) {
        if(decorator != null) {
            decorators = (SetupMessageDecorator<T>[]) ArrayUtils.add(decorators, decorator);
        }
        return this;
    }

    public <A> SetupLib<T> registerCustomType(Class<A> customType, CustomTypeBuilder<A> builder) {
        customTypes.put(customType, builder);
        return this;
    }

    public SetupLib<T> onFinish(FinishHandler<T> finishHandler) {
        this.finishHandler = finishHandler;
        return this;
    }

    public SetupLib<T> onError(ErrorHandler<T> errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    public CompletableFuture<T> start(Player player) {
        Preconditions.checkState(hasNext(), "Cannot start empty setup!");
        SetupLibListener listener = LISTENERS.get(plugin.getName());
        Preconditions.checkState(listener != null, "Plugin is not initialized!");
        SetupLib<T> clone = clone();
        try {
            clone.doNext(player);
        } catch (SetupException e) {
            // Idk what happened here XD
            throw new RuntimeException(e);
        }
        listener.register(player.getUniqueId(), clone);

        // For better user experience, I save future to use.
        CompletableFuture<T> future = new CompletableFuture<>();
        futures.add(future);
        return future;
    }

    /**
     * Makes root step task.
     * This initially handles next part or finish handler.
     *
     * @param player The player to handle this task for.
     * @return true if this task finished setup, otherwise false.
     *
     * @throws SetupException If something occurred.
     */
    protected boolean doNext(Player player) throws SetupException {
        SetupPart<T> next = next();
        if(next == null) {
            T target;
            try {
                target = this.target.getConstructor().newInstance();
            } catch(Exception e) {
                // For now, only no args constructor is accepted.
                throw new SetupException(this, e, "Cannot instantinate target!");
            }

            for(String fieldName : cache.keySet()) {
                Object obj = cache.get(fieldName);
                try {
                    Field field = this.target.getField(fieldName);
                    field.setAccessible(true);
                    field.set(target, obj);

                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new SetupException(this, e, "Cannot fill field " + fieldName + "!");
                }
            }

            finishHandler.onFinish(player, target);

            // We can also have some futures to complete.
            for(CompletableFuture<T> future : futures) {
                future.complete(target);
            }
            return true;
        } else {
            current = next;
            next.send(player);
            return false;
        }
    }

    @Override
    public SetupPart<T> next() {
        if(!hasNext()) {
            return null;
        }
        for(Field field : getApplicableFields()) {
            String fieldName = field.getName();
            if(!cache.containsKey(fieldName)) {
                return new SetupPart<>(this, fieldName);
            }
        }
        return null;
    }

    @Override
    public boolean hasNext() {
        for(Field field : getApplicableFields()) {
            if(!cache.containsKey(field.getName())) {
                return true;
            }
        }
        return false;
    }

    protected void handleError(Player player, Throwable err) {
        errorHandler.onError(player, err);
    }

    private void checkSetup(Class<T> target) {
        boolean anyPart = false;
        for(Field field : getApplicableFields()) {
            if(field.isAnnotationPresent(Setup.class)) {
                anyPart = true;
                break;
            }
        }
        if(!anyPart) {
            throw new NotSetupException(target);
        }
    }

    private Field[] getApplicableFields() {
        Field[] fields = new Field[0];
        for(Field field : target.getDeclaredFields()) {
            if(Primitives.isWrapperType(field.getType())) {
                fields = (Field[]) ArrayUtils.add(fields, field);
            }
        }
        return fields;
    }

    public SetupLib<T> clone() {
        return new SetupLib<>(target,
                plugin,
                new HashMap<>(cache),
                Arrays.copyOf(decorators, decorators.length),
                customTypes,
                finishHandler,
                errorHandler,
                Collections.synchronizedList(new ArrayList<>()),
                current);
    }

    protected static void send(Player player, String s) {
        s = ChatColor.translateAlternateColorCodes('&', s);
        player.sendMessage(s);
    }

    public interface SetupMessageDecorator<T> {
        String[] modify(SetupPart<T> part, String[] message);
    }

    public interface FinishHandler<T> {
        void onFinish(Player player, T result);
    }

    public interface ErrorHandler<T> {
        void onError(Player player, Throwable err);
    }

    public interface CustomTypeBuilder<T> {
        T build(Player player, String arg) throws InputNotAcceptibleException;
    }

}