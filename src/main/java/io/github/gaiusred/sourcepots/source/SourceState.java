package io.github.gaiusred.sourcepots.source;

public final class SourceState {
    int jarRemainingTicks;
    int jarRetryCooldownTicks;
    int boostCooldownTicks;

    public int jarRemainingTicks() {
        return jarRemainingTicks;
    }

    public int jarRetryCooldownTicks() {
        return jarRetryCooldownTicks;
    }

    public int boostCooldownTicks() {
        return boostCooldownTicks;
    }

    /** Restores saved timers without narrowing overflowing long values or advancing time. */
    public void restore(long jarRemaining, long jarRetry, long boostCooldown) {
        jarRemainingTicks = boundedTimer(jarRemaining);
        jarRetryCooldownTicks = boundedTimer(jarRetry);
        boostCooldownTicks = boundedTimer(boostCooldown);
    }

    private static int boundedTimer(long ticks) {
        return (int) Math.clamp(ticks, 0L, Integer.MAX_VALUE);
    }

    void advanceTimers() {
        jarRemainingTicks = decrement(jarRemainingTicks);
        jarRetryCooldownTicks = decrement(jarRetryCooldownTicks);
        boostCooldownTicks = decrement(boostCooldownTicks);
    }

    private static int decrement(int ticks) {
        return ticks > 0 ? ticks - 1 : 0;
    }
}
