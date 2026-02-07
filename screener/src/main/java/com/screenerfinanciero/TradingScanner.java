package com.screenerfinanciero;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

public class TradingScanner {

    public AnalysisResult analyzeAsset(BarSeries series) {
        //Defining the indicators
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma50 = new SMAIndicator(closePrice, 50);
        SMAIndicator sma200 = new SMAIndicator(closePrice, 200);
        RSIIndicator rsi14 = new RSIIndicator(closePrice, 14);

        StringBuilder reason = new StringBuilder();

        // Golden cross logic (SMA 50 > SMA 200)
        boolean goldenCross = sma50.getValue(series.getEndIndex())
            .isGreaterThan(sma200.getValue(series.getEndIndex()));
        if (!goldenCross) reason.append("[Falla Cruce Dorado (SMA50 < SMA200)] ");

        // Momentun filter (RSI between 50 and 65)
        double currentRsi = rsi14.getValue(series.getEndIndex()).doubleValue();
        boolean rsiInZone = currentRsi >= 50 && currentRsi <= 65;
        if (!rsiInZone) reason.append(String.format("[RSI fuera de rango: %.2f] ", currentRsi));

        // Weekly volatility filter
        double volatility = calculateVolatility(series);

        // Buy signal
        boolean volatileEnough = volatility > 0.03;
        if (!volatileEnough) reason.append(String.format("[Volatilidad baja: %.2f%%] ", volatility * 100));

        boolean isEntry = goldenCross && rsiInZone && volatileEnough;       

        // Exit signal (Stop loss 2% under SMA 200)
        double stopLoss = sma200.getValue(series.getEndIndex()).doubleValue() * 0.98;

        return new AnalysisResult(isEntry, currentRsi, stopLoss, volatility, reason.toString());
    }

    private double calculateVolatility(BarSeries series) {
        // Simple logic: (High - Low) / Last Week Close
        return (series.getBar(series.getEndIndex()).getHighPrice().doubleValue() -
            series.getBar(series.getEndIndex()).getLowPrice().doubleValue()) /
            series.getBar(series.getEndIndex()).getClosePrice().doubleValue();
    }

}
