package io.github.gaiusred.sourcepots.source;

public interface SourceAccess {
    boolean canPay(SourceMode mode);

    boolean pay(SourceMode mode);
}
