package com.screenerfinanciero;

public record AnalysisResult(
    boolean isEntrySignal,
    double rsi,
    double stopLoss,
    double volatility,
    String reason
) {
}
