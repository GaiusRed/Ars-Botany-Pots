package io.github.gaiusred.sourcepots.source;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UnavailableGasStorageTest {
    @Test
    void retainedFullBufferCannotFundGrowth() {
        var storage = new UnavailableGasStorage(250);
        storage.restore(250);
        assertFalse(storage.available());
        assertFalse(storage.pay());
        storage.pullAdjacent();
        assertEquals(250, storage.stored());
    }

    @Test
    void restorationClampsToConfiguredCapacity() {
        var storage = new UnavailableGasStorage(100);
        storage.restore(Long.MAX_VALUE);
        assertEquals(100, storage.stored());
        storage.restore(-1);
        assertEquals(0, storage.stored());
        storage.restore(80);
        assertEquals(80, storage.stored());
    }

    @Test
    void longGasQuantitiesRemainExact() {
        var storage = new UnavailableGasStorage(Long.MAX_VALUE);
        storage.restore(Long.MAX_VALUE - 1);
        assertEquals(Long.MAX_VALUE - 1, storage.stored());
        assertEquals(Long.MAX_VALUE, storage.capacity());
    }

    @Test
    void invalidCapacityIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new UnavailableGasStorage(0));
        assertThrows(IllegalArgumentException.class, () -> new UnavailableGasStorage(-1));
    }
}
