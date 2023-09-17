package me.zort.setuplib;

public interface SetupLoader<T> {

    SetupPart<T>[] load();

}
