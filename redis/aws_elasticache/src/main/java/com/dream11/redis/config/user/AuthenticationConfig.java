package com.dream11.redis.config.user;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

    @Size(min = 16, max = 128, message = "authToken must be between 16 and 128 characters")
    @Pattern(regexp = "^[a-zA-Z0-9!&#$<>^-]{16,128}$", message = "authToken must contain only alphanumeric characters and allowed special characters (!, &, #, $, ^, <, >, -)")
    private String authToken;

    @AssertTrue(message = "authToken is required when authentication is enabled")
    boolean isAuthTokenValid() {
        if (enabled != null && enabled) {
            return authToken != null && !authToken.isEmpty();
        }
        return true;
    }
}
