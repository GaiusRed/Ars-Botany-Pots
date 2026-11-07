package io.github.gaiusred.sourcepots.source;

import net.minecraft.core.Direction;
import java.util.EnumMap;

/** Retains saved gas while its API or resource is absent. */
public final class UnavailableGasStorage implements GasStorage {
    private final long capacity;
    private final EnumMap<Direction, Long> stored = new EnumMap<>(Direction.class);

    public UnavailableGasStorage(long capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be positive");
        this.capacity = capacity;
    }

    public boolean available() { return false; }
    public long stored(Direction side) { return stored.getOrDefault(side, 0L); }
    public long capacity() { return capacity; }
    public void restore(Direction side, long amount) {
        if (side != null && side.getAxis().isHorizontal()) stored.put(side, Math.clamp(amount, 0L, capacity));
    }
    public boolean canPay(Direction side) { return false; }
    public boolean pay(Direction side) { return false; }
    public void pullAdjacent() {}
}
