package io.github.gaiusred.sourcepots.source;

public interface SourceAccess {
    int fundedSideMask(SourceMode mode);
    boolean pay(SourceMode mode, int sideMask);
    boolean canPayJar();
    boolean payJar();
}
