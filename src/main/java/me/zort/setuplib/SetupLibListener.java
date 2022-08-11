package me.zort.setuplib;

import com.google.common.collect.Maps;
import me.zort.setuplib.exception.InputNotAcceptibleException;
import me.zort.setuplib.exception.SetupException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static org.bukkit.Bukkit.getServer;

public class SetupLibListener implements Listener {

    private final Plugin plugin;
    private final Map<UUID, SetupLib<?>> setups;

    protected SetupLibListener(Plugin plugin) {
        this.plugin = plugin;
        this.setups = Maps.newConcurrentMap();
        getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void register(UUID uuid, SetupLib<?> setup) {
        setups.put(uuid, setup);
    }

    public Optional<SetupLib<?>> getCurrent(Player player) {
        return Optional.ofNullable(setups.get(player.getUniqueId()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        Optional<SetupLib<?>> setupOptional = getCurrent(player);
        if(setupOptional.isPresent()) {
            SetupLib<?> setup = setupOptional.get();
            e.setCancelled(true);

            try {
                SetupPart<?> current = setup.getCurrent();
                RequiredType type = current.getType();
                if(type != null) {
                    Object obj = type.parse(e.getMessage());

                    if(obj == null) {
                        // Invalid format.
                        for(String s : current.getAnnot().invalidFormat()) {
                            SetupLib.send(player, s);
                        }
                        return;
                    }
                    current.set(obj);
                } else {
                    SetupLib.CustomTypeBuilder<?> builder = null;
                    for(Class<?> customType : setup.getCustomTypes().keySet()) {
                        if(customType.isAssignableFrom(current.getField().getType())) {
                            builder = setup.getCustomTypes().get(customType);
                            break;
                        }
                    }

                    if(builder == null) {
                        // Wot?
                        throw new SetupException(setup, "No custom type builder found for " + current.getField().getType().getName());
                    }

                    try {
                        current.set(builder.build(player, e.getMessage()));
                    } catch (InputNotAcceptibleException ex) {
                        // Custom error.
                        for(String s : ex.getMessageLines()) {
                            SetupLib.send(player, s);
                        }
                    }
                }

                boolean finished;
                try {
                    finished = setup.doNext(player);
                } catch(Exception ex) {
                    ex.printStackTrace();

                    // We need to close the setup now.
                    finished = false;
                    handleSetupClose(player, ex);
                }
                if(finished) {
                    handleSetupClose(player, null);
                }
            } catch (SetupException ex) {
                handleSetupClose(player, ex);
            }
        }
    }

    private void handleSetupClose(Player player, @Nullable Throwable err) {
        SetupLib<?> setup = setups.remove(player.getUniqueId());
        if(setup != null && err != null) {
            setup.handleError(player, err);
        }
    }

    @EventHandler
    public void onDisable(PluginDisableEvent e) {
        if(e.getPlugin().equals(plugin)) {
            List<UUID> toRem = new ArrayList<>();
            for(UUID uuid : setups.keySet()) {
                if(setups.get(uuid).getPlugin().equals(plugin)) {
                    toRem.add(uuid);
                }
            }

            // Clearing instances by the plugin.
            toRem.forEach(setups::remove);
            SetupLib.clear(plugin);
        }
    }

}
