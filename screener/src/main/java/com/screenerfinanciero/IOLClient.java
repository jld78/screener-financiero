package com.screenerfinanciero;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.FormBody;
import okhttp3.OkHttp;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class IOLClient {
    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private String accessToken;
    private String refreshToken;

    // 1. Obtain initial Token (login)
    public void login(String username, String password) throws IOException {
        RequestBody formBody = new FormBody.Builder()
            .add("username", username)
            .add("password", password)
            .add("grant_type", "password")
            .build();

         executeTokenRequest(formBody);
         System.out.println("Login exitoso en IOL");
    }

    // 2. Refresh Token
    public void refreshToken() throws IOException {
        if (refreshToken == null) throw new IllegalStateException("No hay refresh token disponible");

        RequestBody formBody = new FormBody.Builder()
            .add("refresh_token", refreshToken)
            .add("grant_type", "refresh_token")
            .build();

        executeTokenRequest(formBody);
        System.out.println("🔄 Token refrescado correctamente.");
    }

    private void executeTokenRequest(RequestBody body) throws IOException {
        Request request = new Request.Builder()
            .url("https://api.nvertironline.com/token")
            .post(body)
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if(!response.isSuccessful()) throw new IOException("Error en autenticación " + response.message());

            IOLTokenResponse tokenData = mapper.readValue(response.body().string(), IOLTokenResponse.class);
            this.accessToken = tokenData.accessToken();
            this.refreshToken = tokenData.refreshToken();
        }
    }
    
    // 3. Query NYSE/NASDAQ (Historic quotation)
    public String getHistoricalData(String symbol, String market) throws IOException {
        // Market: NYSE or NASDAQ
        String url = String.format("https://api.invertironline.com/api/v2/Titulos/%s/%s/CotizacionHistorica", market, symbol);

        Request request = new Request.Builder()
            .url(url)
            .addHeader("Authoriation", "Bearer" + accessToken)
            .get()
            .build();

        try(Response response = httpClient.newCall(request).execute()) {
            if (response.code() == 401) { // Unauthorized the token has expired
                refreshToken();
                return getHistoricalData(symbol, market); // Retry
            } 
            
            if (!response.isSuccessful()) {
                throw new IOException("Error en la solicitud: " + response.message());
            }
            return response.body().string();
        }    
    }
}
