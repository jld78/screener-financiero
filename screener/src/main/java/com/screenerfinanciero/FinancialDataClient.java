package com.screenerfinanciero;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;

public class FinancialDataClient {
    private static final OkHttpClient client = new OkHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String API_KEY = System.getenv("FINANCIALDATA_API_KEY");
    private static final String BASE_URL = "https://financialdata.net/api/v1/stock_prices";

    public BarSeries fetchSeries(String symbol, String period) throws IOException {
        if (API_KEY == null || API_KEY.isEmpty()) {
            throw new IOException("FINANCIALDATA_API_KEY environment variable not set.");
        }

        String url = String.format("%s?symbols=%s&period=%s&api_key=%s", BASE_URL, symbol, period, API_KEY);

        Request request = new Request.Builder()
            .url(url)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String responseBody = response.body().string();
            JsonNode root = mapper.readTree(responseBody);

            if (root.has("error")) {
                throw new IOException("FinancialData API error: " + root.get("error").asText());
            }

            JsonNode dataNode = root.get("data");
            if (dataNode == null || !dataNode.isArray()) {
                throw new IOException("Data not found or is not an array in response for symbol: " + symbol);
            }

            BarSeries series = new BaseBarSeriesBuilder().withName(symbol).build();
            List<JsonNode> dailyData = new ArrayList<>();
            dataNode.elements().forEachRemaining(dailyData::add);

            // Sort by date to ensure correct order for BarSeries
            Collections.sort(dailyData, Comparator.comparing(node -> LocalDate.parse(node.get("date").asText())));

            for (JsonNode data : dailyData) {
                LocalDate date = LocalDate.parse(data.get("date").asText());
                series.addBar(ZonedDateTime.of(date.atStartOfDay(), ZoneId.systemDefault()),
                    data.get("open").asDouble(),
                    data.get("high").asDouble(),
                    data.get("low").asDouble(),
                    data.get("close").asDouble(),
                    data.get("volume").asDouble()
                );
            }
            return series;
        }
    }
}