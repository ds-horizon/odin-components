package com.dream11.application.config.user;

import com.dream11.application.constant.Constants;
import com.dream11.application.constant.DiscoveryType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.NullNode;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonDeserialize(using = DiscoveryConfig.DiscoveryConfigDeserializer.class)
public class DiscoveryConfig {

  @JsonProperty("public")
  String publicRoute;

  @JsonProperty("private")
  String privateRoute;

  @JsonIgnore @NotNull DiscoveryType type;

  public static class DiscoveryConfigDeserializer extends StdDeserializer<DiscoveryConfig> {

    protected DiscoveryConfigDeserializer(Class<?> vc) {
      super(vc);
    }

    public DiscoveryConfigDeserializer() {
      this(null);
    }

    @Override
    public DiscoveryConfig deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException {
      JsonNode node = jp.getCodec().readTree(jp);
      String publicRoute =
          node.get(Constants.PUBLIC) == null || node.get(Constants.PUBLIC) instanceof NullNode
              ? null
              : node.get(Constants.PUBLIC).asText();
      String privateRoute =
          node.get(Constants.PRIVATE) == null || node.get(Constants.PRIVATE) instanceof NullNode
              ? null
              : node.get(Constants.PRIVATE).asText();

      DiscoveryConfigBuilder builder =
          DiscoveryConfig.builder().publicRoute(publicRoute).privateRoute(privateRoute);
      if (publicRoute != null && privateRoute != null) {
        builder.type(DiscoveryType.BOTH);
      } else if (privateRoute != null) {
        builder.type(DiscoveryType.PRIVATE);
      } else if (publicRoute != null) {
        builder.type(DiscoveryType.PUBLIC);
      } else {
        builder.type(DiscoveryType.NONE);
      }
      return builder.build();
    }
  }
}
