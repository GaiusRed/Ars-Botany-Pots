package io.github.gaiusred.sourcepots.source;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceSchedulerTest {
    private final SourceScheduler scheduler = new SourceScheduler();
    private final SourceState state = new SourceState();
    private final SourceSettings defaults = SourceSettings.defaults();

    @Test
    void defaultGasBoostsEveryTick() {
        CountingAccess access = new CountingAccess(SourceMode.GAS);

        assertEquals(List.of(0, 1, 2, 3, 4), boostTicks(defaults, access, 0, 4, true));
        assertEquals(5, access.payments(SourceMode.GAS));
    }

    @Test
    void defaultLiquidBoostsEveryFiveTicks() {
        CountingAccess access = new CountingAccess(SourceMode.LIQUID);

        assertEquals(List.of(0, 5, 10, 15), boostTicks(defaults, access, 0, 19, true));
        assertEquals(4, access.payments(SourceMode.LIQUID));
    }

    @Test
    void jarPaysAt0And80() {
        CountingAccess access = new CountingAccess(SourceMode.JAR);

        assertEquals(List.of(0, 10, 20, 30, 40, 50, 60, 70), boostTicks(defaults, access, 0, 79, true));
        assertEquals(1, access.payments(SourceMode.JAR));
        assertEquals(21, state.jarRemainingTicks());
        org.junit.jupiter.api.Assertions.assertNotNull(scheduler.tick(state, defaults, true, access));
        assertEquals(2, access.payments(SourceMode.JAR));
        assertEquals(100, state.jarRemainingTicks());
        assertEquals(80, state.jarRetryCooldownTicks());
    }

    @Test
    void jarRenewsTo100Not200() {
        CountingAccess access = new CountingAccess(SourceMode.JAR);

        boostTicks(defaults, access, 0, 80, true);

        assertEquals(100, state.jarRemainingTicks());
        assertEquals(2, access.payments(SourceMode.JAR));
    }

    @Test
    void failedRenewalKeepsPaidRemainder() {
        CountingAccess access = new CountingAccess(SourceMode.JAR);
        boostTicks(defaults, access, 0, 79, true);
        access.funded.clear();

        org.junit.jupiter.api.Assertions.assertNotNull(scheduler.tick(state, defaults, true, access));
        assertEquals(20, state.jarRemainingTicks());
        assertEquals(80, state.jarRetryCooldownTicks());
        assertEquals(2, access.attempts(SourceMode.JAR));
        assertEquals(1, access.payments(SourceMode.JAR));
        assertEquals(List.of(90), boostTicks(defaults, access, 81, 100, true));
        assertEquals(0, state.jarRemainingTicks());
    }

    @Test
    void invalidGrowthNeverPays() {
        CountingAccess access = new CountingAccess(SourceMode.GAS, SourceMode.LIQUID, SourceMode.JAR);

        assertEquals(List.of(), boostTicks(defaults, access, 0, 160, false));
        for (SourceMode mode : SourceMode.values()) {
            assertEquals(0, access.attempts(mode));
        }
        assertEquals(0, state.jarRemainingTicks());
    }

    @Test
    void fasterInputWaitsForExistingCooldown() {
        CountingAccess access = new CountingAccess(SourceMode.JAR);
        org.junit.jupiter.api.Assertions.assertNotNull(scheduler.tick(state, defaults, true, access));
        access.funded.add(SourceMode.GAS);

        assertEquals(List.of(), boostTicks(defaults, access, 1, 9, true));
        assertEquals(0, access.payments(SourceMode.GAS));
        assertEquals(List.of(10, 11, 12, 13, 14, 15), boostTicks(defaults, access, 10, 15, true));
        assertEquals(6, access.payments(SourceMode.GAS));
        assertEquals(1, access.payments(SourceMode.JAR));
    }

    @Test
    void oneBoostPerTick() {
        CountingAccess access = new CountingAccess(SourceMode.GAS, SourceMode.LIQUID, SourceMode.JAR);

        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
                boostTicks(defaults, access, 0, 9, true));
        assertEquals(10, access.payments(SourceMode.GAS));
        assertEquals(0, access.attempts(SourceMode.LIQUID));
        assertEquals(0, access.attempts(SourceMode.JAR));
    }

    @Test
    void configuredIntervalsDoNotChangePriority() {
        CountingAccess access = new CountingAccess(SourceMode.GAS, SourceMode.LIQUID, SourceMode.JAR);
        SourceSettings settings = intervals(20, 1, 9);

        assertEquals(List.of(0, 9), boostTicks(settings, access, 0, 9, true));
        assertEquals(0, access.payments(SourceMode.LIQUID));
        assertEquals(2, access.attempts(SourceMode.GAS));
        assertEquals(0, access.attempts(SourceMode.JAR));
    }

    @Test
    void configuredFastJarStillLosesToGas() {
        CountingAccess access = new CountingAccess(SourceMode.GAS, SourceMode.LIQUID, SourceMode.JAR);
        SourceSettings settings = intervals(2, 3, 5);

        assertEquals(List.of(0, 5), boostTicks(settings, access, 0, 9, true));
        assertEquals(0, access.payments(SourceMode.JAR));
        assertEquals(2, access.attempts(SourceMode.GAS));
        assertEquals(0, access.attempts(SourceMode.LIQUID));
    }

    @Test
    void tiesPreferGasThenLiquid() {
        CountingAccess access = new CountingAccess(SourceMode.GAS, SourceMode.LIQUID, SourceMode.JAR);
        SourceSettings settings = intervals(5, 5, 5);
        org.junit.jupiter.api.Assertions.assertNotNull(scheduler.tick(state, settings, true, access));
        access.funded.remove(SourceMode.GAS);
        assertEquals(List.of(5), boostTicks(settings, access, 1, 5, true));
        access.funded.remove(SourceMode.LIQUID);
        assertEquals(List.of(10), boostTicks(settings, access, 6, 10, true));

        for (SourceMode mode : SourceMode.values()) {
            assertEquals(1, access.payments(mode));
        }
    }

    @Test
    void jarExpiresDuringGasAndInvalidGrowth() {
        CountingAccess access = new CountingAccess(SourceMode.JAR);
        org.junit.jupiter.api.Assertions.assertNotNull(scheduler.tick(state, defaults, true, access));
        access.funded.add(SourceMode.GAS);
        assertEquals(java.util.stream.IntStream.rangeClosed(10, 50).boxed().toList(), boostTicks(defaults, access, 1, 50, true));
        assertEquals(50, state.jarRemainingTicks());
        assertEquals(List.of(), boostTicks(defaults, access, 51, 100, false));

        assertEquals(0, state.jarRemainingTicks());
        assertEquals(0, state.jarRetryCooldownTicks());
        assertEquals(0, state.boostCooldownTicks());
        assertEquals(1, access.payments(SourceMode.JAR));
        assertEquals(41, access.payments(SourceMode.GAS));
        access.funded.clear();
        org.junit.jupiter.api.Assertions.assertNull(scheduler.tick(state, defaults, true, access));
    }

    @Test
    void emptyInputsProduceNoBoost() {
        CountingAccess access = new CountingAccess();

        assertEquals(List.of(), boostTicks(defaults, access, 0, 160, true));
        for (SourceMode mode : SourceMode.values()) {
            assertEquals(0, access.attempts(mode));
        }
    }

    @Test
    void failedJarPaymentRetriesAfter80Ticks() {
        CountingAccess access = new CountingAccess(SourceMode.JAR);
        access.failPayments.add(SourceMode.JAR);
        org.junit.jupiter.api.Assertions.assertNull(scheduler.tick(state, defaults, true, access));
        assertEquals(1, access.attempts(SourceMode.JAR));
        assertEquals(List.of(), boostTicks(defaults, access, 1, 79, true));
        assertEquals(1, access.attempts(SourceMode.JAR));
        org.junit.jupiter.api.Assertions.assertNull(scheduler.tick(state, defaults, true, access));
        assertEquals(2, access.attempts(SourceMode.JAR));
        assertEquals(2, access.checks(SourceMode.JAR));
        access.failPayments.clear();
        assertEquals(List.of(), boostTicks(defaults, access, 81, 159, true));
        assertEquals(2, access.attempts(SourceMode.JAR));
        org.junit.jupiter.api.Assertions.assertNotNull(scheduler.tick(state, defaults, true, access));
        assertEquals(3, access.attempts(SourceMode.JAR));
        assertEquals(1, access.payments(SourceMode.JAR));
        assertEquals(100, state.jarRemainingTicks());
    }

    @Test
    void unpaidJarOnRetryDoesNotBlockSlowerFundedInput() {
        CountingAccess access = new CountingAccess(SourceMode.JAR);
        SourceSettings settings = intervals(2, 3, 5);
        access.failPayments.add(SourceMode.JAR);
        org.junit.jupiter.api.Assertions.assertNull(scheduler.tick(state, settings, true, access));
        access.funded.add(SourceMode.LIQUID);

        assertEquals(List.of(1, 4, 7), boostTicks(settings, access, 1, 9, true));
        assertEquals(3, access.payments(SourceMode.LIQUID));
        assertEquals(1, access.attempts(SourceMode.JAR));
    }

    @Test
    void jarRenewsIndependentlyOfBoostCooldown() {
        CountingAccess access = new CountingAccess(SourceMode.JAR);
        SourceSettings settings = intervals(30, 10, 5);
        assertEquals(List.of(0, 30, 60), boostTicks(settings, access, 0, 79, true));

        org.junit.jupiter.api.Assertions.assertNull(scheduler.tick(state, settings, true, access));
        assertEquals(2, access.payments(SourceMode.JAR));
        assertEquals(100, state.jarRemainingTicks());
        assertEquals(10, state.boostCooldownTicks());
        assertEquals(List.of(90), boostTicks(settings, access, 81, 90, true));
    }

    @Test
    void paidJarResumesWithoutNewPaymentAfterFasterInputRunsOut() {
        CountingAccess access = new CountingAccess(SourceMode.JAR);
        org.junit.jupiter.api.Assertions.assertNotNull(scheduler.tick(state, defaults, true, access));
        access.funded.clear();
        access.funded.add(SourceMode.GAS);
        assertEquals(java.util.stream.IntStream.rangeClosed(10, 25).boxed().toList(), boostTicks(defaults, access, 1, 25, true));
        access.funded.clear();

        assertEquals(List.of(26, 36, 46), boostTicks(defaults, access, 26, 50, true));
        assertEquals(1, access.payments(SourceMode.JAR));
        assertEquals(1, access.attempts(SourceMode.JAR));
        assertEquals(50, state.jarRemainingTicks());
    }

    @Test
    void insufficientJarFundingChecksRetryAfter80Ticks() {
        CountingAccess access = new CountingAccess();
        org.junit.jupiter.api.Assertions.assertNull(scheduler.tick(state, defaults, true, access));
        assertEquals(1, access.checks(SourceMode.JAR));
        assertEquals(80, state.jarRetryCooldownTicks());
        assertEquals(List.of(), boostTicks(defaults, access, 1, 79, true));
        assertEquals(1, access.checks(SourceMode.JAR));

        org.junit.jupiter.api.Assertions.assertNull(scheduler.tick(state, defaults, true, access));
        assertEquals(2, access.checks(SourceMode.JAR));
        assertEquals(80, state.jarRetryCooldownTicks());
        assertEquals(0, access.attempts(SourceMode.JAR));
    }

    @Test
    void failedFastJarFundingCheckAllowsSlowerInput() {
        CountingAccess access = new CountingAccess(SourceMode.LIQUID);

        assertEquals(List.of(0, 3, 6, 9), boostTicks(intervals(2, 3, 5), access, 0, 9, true));
        assertEquals(4, access.payments(SourceMode.LIQUID));
        assertEquals(0, access.checks(SourceMode.JAR));
        assertEquals(0, access.attempts(SourceMode.JAR));
    }

    @Test
    void fasterFundedModeSuppressesExistingJarRenewalAndFundingChecks() {
        CountingAccess access = new CountingAccess(SourceMode.JAR);
        org.junit.jupiter.api.Assertions.assertNotNull(scheduler.tick(state, defaults, true, access));
        access.funded.add(SourceMode.GAS);

        assertEquals(java.util.stream.IntStream.rangeClosed(10, 100).boxed().toList(),
                boostTicks(defaults, access, 1, 100, true));
        assertEquals(91, access.payments(SourceMode.GAS));
        assertEquals(1, access.attempts(SourceMode.JAR));
        assertEquals(1, access.checks(SourceMode.JAR));
        assertEquals(0, state.jarRemainingTicks());
    }

    @Test
    void returnsAuthorizedModeForTiesSwitchingAndFailedPayment() {
        CountingAccess access = new CountingAccess(SourceMode.GAS, SourceMode.LIQUID, SourceMode.JAR);
        SourceSettings settings = intervals(5, 5, 5);
        assertEquals(SourceMode.GAS, scheduler.tick(state, settings, true, access).mode());
        access.funded.remove(SourceMode.GAS);
        for (int tick = 1; tick < 5; tick++) {
            org.junit.jupiter.api.Assertions.assertNull(scheduler.tick(state, settings, true, access));
        }
        assertEquals(SourceMode.LIQUID, scheduler.tick(state, settings, true, access).mode());
        access.funded.remove(SourceMode.LIQUID);
        for (int tick = 6; tick < 10; tick++) {
            org.junit.jupiter.api.Assertions.assertNull(scheduler.tick(state, settings, true, access));
        }
        assertEquals(SourceMode.JAR, scheduler.tick(state, settings, true, access).mode());
        var failedState = new SourceState();
        access.funded.add(SourceMode.GAS);
        access.failPayments.add(SourceMode.GAS);
        org.junit.jupiter.api.Assertions.assertNull(scheduler.tick(failedState, settings, true, access));
        assertEquals(0, failedState.boostCooldownTicks());
    }

    private SourceSettings intervals(int jar, int liquid, int gas) {
        return new SourceSettings(2000, 80, 100, jar, 3,
                mode(250, liquid), mode(125, gas), defaults.jarGrowth(), 16);
    }

    private SourceSettings.ModeSettings mode(long cost, int interval) {
        return new SourceSettings.ModeSettings(cost, cost, java.util.Collections.nCopies(4,
                new SourceSettings.TierSettings(interval, defaults.jarGrowth())));
    }

    @Test void masksSelectAggregateTierAndOnlyPaySelectedResource() {
        var access = new CountingAccess(SourceMode.GAS, SourceMode.LIQUID);
        access.gasMask = 7;
        access.liquidMask = 15;
        var boost = scheduler.tick(state, defaults, true, access);
        assertEquals(new SourceScheduler.Boost(SourceMode.GAS, 7, defaults.gas().tier(3)), boost);
        assertEquals(7, access.paidMask);
        assertEquals(0, access.attempts(SourceMode.LIQUID));
        access.gasMask = 1;
        assertEquals(defaults.gas().tier(1), scheduler.tick(state, defaults, true, access).tier());
    }

    @Test void fallbackRetainsSlowerGasCooldown() {
        var access = new CountingAccess(SourceMode.GAS, SourceMode.LIQUID);
        var settings = intervals(10, 1, 9);
        assertEquals(SourceMode.GAS, scheduler.tick(state, settings, true, access).mode());
        access.funded.remove(SourceMode.GAS);
        for (int remaining = 8; remaining > 0; remaining--) {
            assertNull(scheduler.tick(state, settings, true, access));
            assertEquals(remaining, state.boostCooldownTicks());
            assertEquals(0, access.attempts(SourceMode.LIQUID));
        }
        assertEquals(SourceMode.LIQUID, scheduler.tick(state, settings, true, access).mode());
        assertEquals(1, access.payments(SourceMode.LIQUID));
    }

    @Test void failedBatchDoesNotStartCooldownOrFallback() {
        var access = new CountingAccess(SourceMode.GAS, SourceMode.LIQUID);
        access.failPayments.add(SourceMode.GAS);
        assertNull(scheduler.tick(state, defaults, true, access));
        assertEquals(0, state.boostCooldownTicks());
        assertEquals(0, access.attempts(SourceMode.LIQUID));
    }

    private List<Integer> boostTicks(SourceSettings settings, CountingAccess access,
                                     int first, int last, boolean eligible) {
        List<Integer> ticks = new ArrayList<>();
        for (int tick = first; tick <= last; tick++) {
            if (scheduler.tick(state, settings, eligible, access) != null) {
                ticks.add(tick);
            }
        }
        return ticks;
    }

    private static final class CountingAccess implements SourceAccess {
        private final EnumSet<SourceMode> funded = EnumSet.noneOf(SourceMode.class);
        private final EnumSet<SourceMode> failPayments = EnumSet.noneOf(SourceMode.class);
        private final EnumMap<SourceMode, Integer> attempts = new EnumMap<>(SourceMode.class);
        private final EnumMap<SourceMode, Integer> payments = new EnumMap<>(SourceMode.class);
        private final EnumMap<SourceMode, Integer> checks = new EnumMap<>(SourceMode.class);

        private CountingAccess(SourceMode... modes) {
            funded.addAll(List.of(modes));
        }

        private int gasMask = 1;
        private int liquidMask = 1;
        private int paidMask;
        @Override public int fundedSideMask(SourceMode mode) {
            return funded.contains(mode) ? (mode == SourceMode.GAS ? gasMask : liquidMask) : 0;
        }
        @Override public boolean pay(SourceMode mode, int sideMask) { paidMask = sideMask; return payment(mode); }
        @Override public boolean canPayJar() { return canPay(SourceMode.JAR); }
        @Override public boolean payJar() { return payment(SourceMode.JAR); }
        private boolean canPay(SourceMode mode) {
            checks.merge(mode, 1, Integer::sum);
            return funded.contains(mode);
        }

        private boolean payment(SourceMode mode) {
            attempts.merge(mode, 1, Integer::sum);
            if (!funded.contains(mode) || failPayments.contains(mode)) {
                return false;
            }
            payments.merge(mode, 1, Integer::sum);
            return true;
        }

        private int attempts(SourceMode mode) {
            return attempts.getOrDefault(mode, 0);
        }

        private int payments(SourceMode mode) {
            return payments.getOrDefault(mode, 0);
        }

        private int checks(SourceMode mode) {
            return checks.getOrDefault(mode, 0);
        }
    }
}
