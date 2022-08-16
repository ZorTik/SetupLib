package me.zort.setuplib.util;

import me.zort.setuplib.SetupLib;
import me.zort.setuplib.exception.SetupException;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public class QuitErrorHandler<T> implements SetupLib.ErrorHandler<T> {

    private final Consumer<Player> action;

    public QuitErrorHandler(Consumer<Player> action) {
        this.action = action;
    }

    @Override
    public void onError(Player player, Throwable err) {
        if(err instanceof SetupException && err.getMessage().equals("Player left.")) {
            action.accept(player);
        }
    }

}
