package com.screenerfinanciero;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IOLTokenResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("refresh_token") String refreshToken,
    @JsonProperty(".expires") String expires,
    @JsonProperty(".issued") String issued,
    @JsonProperty("token_type") String tokenType
) {

}
