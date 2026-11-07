package io.github.gaiusred.sourcepots.source;

import net.minecraft.core.Direction;

/** Gas operations that do not require the optional chemical API. */
public interface GasStorage {
    boolean available();
    long stored(Direction side);
    long capacity();
    void restore(Direction side, long amount);
    boolean canPay(Direction side);
    boolean pay(Direction side);
    void pullAdjacent();
}
