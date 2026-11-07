package io.github.gaiusred.sourcepots.source;

import org.junit.jupiter.api.Test;
import net.minecraft.core.Direction;
import static org.junit.jupiter.api.Assertions.*;

class UnavailableGasStorageTest {
    @Test
    void retainedFullBufferCannotFundGrowth() {
        var storage = new UnavailableGasStorage(250);
        storage.restore(Direction.NORTH, 250);
        storage.restore(Direction.EAST, 50);
        assertEquals(50, storage.stored(Direction.EAST));
        assertFalse(storage.canPay(Direction.NORTH));
        assertFalse(storage.available());
        assertFalse(storage.pay(Direction.NORTH));
        storage.pullAdjacent();
        assertEquals(250, storage.stored(Direction.NORTH));
    }

    @Test
    void restorationClampsToConfiguredCapacity() {
        var storage = new UnavailableGasStorage(100);
        storage.restore(Direction.NORTH, Long.MAX_VALUE);
        assertEquals(100, storage.stored(Direction.NORTH));
        storage.restore(Direction.NORTH, -1);
        assertEquals(0, storage.stored(Direction.NORTH));
        storage.restore(Direction.NORTH, 80);
        assertEquals(80, storage.stored(Direction.NORTH));
    }

    @Test
    void longGasQuantitiesRemainExact() {
        var storage = new UnavailableGasStorage(Long.MAX_VALUE);
        storage.restore(Direction.NORTH, Long.MAX_VALUE - 1);
        assertEquals(Long.MAX_VALUE - 1, storage.stored(Direction.NORTH));
        assertEquals(Long.MAX_VALUE, storage.capacity());
    }

    @Test
    void invalidCapacityIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new UnavailableGasStorage(0));
        assertThrows(IllegalArgumentException.class, () -> new UnavailableGasStorage(-1));
    }
}
