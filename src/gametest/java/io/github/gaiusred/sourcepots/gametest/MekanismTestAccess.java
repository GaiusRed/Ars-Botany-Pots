package io.github.gaiusred.sourcepots.gametest;

import io.github.gaiusred.sourcepots.compat.mekanism.MekanismSourceStorage;
import io.github.gaiusred.sourcepots.source.PotSourceAccess;
import mekanism.api.chemical.IChemicalHandler;

/** Full-integration tests deliberately use the optional API through this boundary. */
final class MekanismTestAccess {
    static IChemicalHandler chemical(PotSourceAccess access) {
        return ((MekanismSourceStorage) access.gasStorage()).handler();
    }
    private MekanismTestAccess() {}
}
