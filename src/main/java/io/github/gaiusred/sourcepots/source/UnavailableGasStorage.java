package io.github.gaiusred.sourcepots.source;

/** Retains saved gas while its API or resource is absent. */
public final class UnavailableGasStorage implements GasStorage {
    private final long capacity;
    private long stored;

    public UnavailableGasStorage(long capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be positive");
        this.capacity = capacity;
    }

    public boolean available() { return false; }
    public long stored() { return stored; }
    public long capacity() { return capacity; }
    public void restore(long amount) { stored = Math.clamp(amount, 0L, capacity); }
    public boolean pay() { return false; }
    public void pullAdjacent() {}
}
