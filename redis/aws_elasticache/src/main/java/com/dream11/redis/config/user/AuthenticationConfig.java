package com.dream11.redis.config.user;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationConfig {
    @NotNull
    @Default
    private Boolean enabled = false;

    private String authToken;

    @AssertTrue(message = "authToken is required when authentication is enabled")
    boolean isAuthTokenValid() {
        if (enabled != null && enabled) {
            return authToken != null && !authToken.isEmpty();
        }
        return true;
    }
}
