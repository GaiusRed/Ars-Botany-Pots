package io.github.gaiusred.sourcepots.source;

public final class SourceScheduler {
    public record Boost(SourceMode mode, int sideMask, SourceSettings.TierSettings tier) {}

    /** Advances real-tick timers once, then authorizes at most one aggregate boost. */
    public Boost tick(SourceState state, SourceSettings settings, boolean growthEligible, SourceAccess access) {
        state.advanceTimers();
        if (!growthEligible) return null;

        int mask = access.fundedSideMask(SourceMode.GAS);
        SourceMode selected = SourceMode.GAS;
        if (mask == 0) {
            mask = access.fundedSideMask(SourceMode.LIQUID);
            selected = SourceMode.LIQUID;
        }
        if (mask == 0) {
            selected = SourceMode.JAR;
            if (state.jarRetryCooldownTicks == 0) {
                state.jarRetryCooldownTicks = settings.jarPaymentIntervalTicks();
                if (state.jarRemainingTicks > 0 || access.canPayJar()) {
                    if (access.payJar()) state.jarRemainingTicks = settings.jarPoweredDurationTicks();
                }
            }
            if (state.jarRemainingTicks == 0) return null;
        }
        if (state.boostCooldownTicks > 0) return null;

        var tier = settings.tier(selected, Integer.bitCount(mask));
        if (selected != SourceMode.JAR && !access.pay(selected, mask)) return null;
        state.boostCooldownTicks = tier.intervalTicks();
        return new Boost(selected, mask, tier);
    }
}
