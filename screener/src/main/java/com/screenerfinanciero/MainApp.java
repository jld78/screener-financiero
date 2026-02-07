package com.screenerfinanciero;

import java.util.List;

import org.ta4j.core.BarSeries;

public class MainApp {
    public static void main(String[] args) {
        List<String> watchlist = List.of("NVDA", "MSFT", "PLTR", "AMD", "AVGO", "TSLA", "META", "AMZN", "SPY", "QQQ");
        TradingScanner scanner = new TradingScanner();
        AlphaVantageClient client = new AlphaVantageClient();
        FinancialDataClient financialDataClient = new FinancialDataClient();

        System.out.println("--- Executing Trading Scanner ---");

        for (String symbol : watchlist) {
            try {
                BarSeries series = client.fetchSeries(symbol);
                AnalysisResult result = scanner.analyzeAsset(series);

                if (result.isEntrySignal()) {
                    System.out.printf("Señal en %s: RSI %2.f | SL Sugerido: %.2 | Volatilidad: %.2f%%%n", 
                        symbol, result.rsi(), result.stopLoss(), result.volatility() * 100
                    );
                } else {
                    System.out.printf("❌ %s: %s%n", symbol, result.reason());
                }

                // Add a 1-second delay to respect Alpha Vantage API limits
                try {
                    Thread.sleep(1000); 
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Thread interrupted: " + e.getMessage());
                }

            } catch (Exception e) {
                System.err.println("Error analizando " + symbol + ": " + e.getMessage());
            }
        }


    }
}