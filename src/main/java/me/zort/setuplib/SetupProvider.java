package me.zort.setuplib;

import lombok.AllArgsConstructor;
import org.bukkit.plugin.Plugin;

/**
 * Provider for simplicity of setups
 * creation usage.
 */
@AllArgsConstructor
public class SetupProvider {

    private final Plugin plugin;

    public <T> SetupLib<T> create(Class<T> clazz) {
        return new SetupLib<>(plugin, clazz);
    }

}
