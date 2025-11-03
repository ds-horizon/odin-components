package com.dream11.application.config.metadata.aws;

import com.dream11.application.config.Config;
import com.dream11.application.error.ApplicationError;
import com.dream11.application.exception.GenericApplicationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscoveryData implements Config {

  @Valid @NotEmpty List<Domain> domains;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Domain {
    @NotBlank String id;
    @NotBlank String name;
    @NotNull Boolean isActive;
    @NotBlank String certificateArn;
  }

  public Domain getDomainFromRoute(String route) {
    return this.domains.stream()
        .filter(domain -> route.endsWith(domain.getName()))
        .findFirst()
        .orElseThrow(
            () ->
                new GenericApplicationException(ApplicationError.NO_DOMAIN_FOUND_FOR_ROUTE, route));
  }
}
