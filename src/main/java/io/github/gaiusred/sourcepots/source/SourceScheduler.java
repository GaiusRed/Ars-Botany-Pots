package io.github.gaiusred.sourcepots.source;

public final class SourceScheduler {
    /**
     * Expires existing timers first, then attempts payment and at most one boost.
     * New timers retain their full interval on the purchase/boost tick, including tick zero.
     */
    public SourceMode tick(SourceState state, SourceSettings settings, boolean growthEligible, SourceAccess access) {
        state.advanceTimers();
        if (!growthEligible) {
            return null;
        }

        SourceMode selected = null;
        for (SourceMode mode : SourceMode.values()) {
            if ((selected == null || interval(mode, settings) < interval(selected, settings))
                    && checkFunding(mode, state, settings, access)) {
                selected = mode;
            }
        }
        if (selected == null) {
            return null;
        }

        if (selected == SourceMode.JAR && state.jarRetryCooldownTicks == 0) {
            state.jarRetryCooldownTicks = settings.jarPaymentIntervalTicks();
            if (access.pay(SourceMode.JAR)) {
                state.jarRemainingTicks = settings.jarPoweredDurationTicks();
            }
        }
        if (state.boostCooldownTicks > 0) {
            return null;
        }

        boolean paid = selected == SourceMode.JAR ? state.jarRemainingTicks > 0 : access.pay(selected);
        if (paid) {
            state.boostCooldownTicks = interval(selected, settings);
        }
        return paid ? selected : null;
    }

    private boolean checkFunding(SourceMode mode, SourceState state, SourceSettings settings, SourceAccess access) {
        if (mode != SourceMode.JAR) {
            return access.canPay(mode);
        }
        if (state.jarRemainingTicks > 0) {
            return true;
        }
        if (state.jarRetryCooldownTicks > 0) {
            return false;
        }
        if (access.canPay(mode)) {
            return true;
        }
        state.jarRetryCooldownTicks = settings.jarPaymentIntervalTicks();
        return false;
    }

    private int interval(SourceMode mode, SourceSettings settings) {
        return switch (mode) {
            case GAS -> settings.gas().intervalTicks();
            case LIQUID -> settings.liquid().intervalTicks();
            case JAR -> settings.jarBoostIntervalTicks();
        };
    }
}
