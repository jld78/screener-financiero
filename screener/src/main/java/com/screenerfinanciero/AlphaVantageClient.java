package com.screenerfinanciero;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AlphaVantageClient {
    private static final OkHttpClient client = new OkHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String API_KEY = System.getenv("ALPHA_VANTAGE_API_KEY");
    private static final String BASE_URL = "https://www.alphavantage.co/query";

    public BarSeries fetchSeries(String symbol) throws IOException {
        String url = BASE_URL + "?function=TIME_SERIES_DAILY&symbol=" + symbol 
        + "&outputsize=compact"
        + "&apikey=" + API_KEY;

        Request request = new Request.Builder()
            .url(url)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            
            String responseBody = response.body().string();
            //System.out.println("Alpha Vantage Response: " + responseBody); // For debugging
            JsonNode root = mapper.readTree(responseBody);

            if (root.has("Error Message")) {
                throw new IOException("Alpha Vantage API error: " + root.get("Error Message").asText());
            }
            if (root.has("Note")) {
                throw new IOException("Alpha Vantage API note: " + root.get("Note").asText());
            }

            JsonNode timeSeries = root.get("Time Series (Daily)");
            if (timeSeries == null) {
                throw new IOException("Time Series (Daily) not found in response for symbol: " + symbol);
            }

            BarSeries series = new BaseBarSeriesBuilder().withName(symbol).build();

            List<Map.Entry<String, JsonNode>> entries = new ArrayList<>();
            timeSeries.fields().forEachRemaining(entries::add);
            Collections.sort(entries, Map.Entry.comparingByKey());

            for (Map.Entry<String, JsonNode> entry : entries) {
                LocalDate date = LocalDate.parse(entry.getKey());
                JsonNode data = entry.getValue();

                series.addBar(ZonedDateTime.of(date.atStartOfDay(), ZoneId.systemDefault()),
                    data.get("1. open").asDouble(),
                    data.get("2. high").asDouble(),
                    data.get("3. low").asDouble(),
                    data.get("4. close").asDouble(),
                    data.get("5. volume").asDouble()
                );
            }

            return series;
        }
    }
}
