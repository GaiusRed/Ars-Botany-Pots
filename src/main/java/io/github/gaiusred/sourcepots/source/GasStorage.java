package io.github.gaiusred.sourcepots.source;

/** Gas operations that do not require the optional chemical API. */
public interface GasStorage {
    boolean available();
    long stored();
    long capacity();
    void restore(long amount);
    boolean pay();
    void pullAdjacent();
}
