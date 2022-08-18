package me.zort.setuplib.impl;

import me.zort.setuplib.SetupLib;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.List;

public class ConfigMessageBuilder implements SetupLib.MessageBuilder {

    private final FileConfiguration config;

    public ConfigMessageBuilder(FileConfiguration config) {
        this.config = config;
    }

    @Override
    public List<String> build(String inPlaceholder) {
        if(inPlaceholder.contains(" ")) {
            // This is not configuration path.
            return Collections.singletonList(inPlaceholder);
        }
        return config.isList(inPlaceholder)
                ? config.getStringList(inPlaceholder)
                : Collections.singletonList(config.getString(inPlaceholder));
    }

}
