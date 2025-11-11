package com.dream11.application.state;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AMIState {
  String artifactSha;
  @Builder.Default List<AMI> amis = new ArrayList<>();

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AMI {
    @NotNull String id;
    @NotNull String architecture;
  }

  public void addAMI(String id, String architecture) {
    Optional<AMI> existingAmi =
        this.amis.stream().filter(ami -> ami.getArchitecture().equals(architecture)).findFirst();
    if (existingAmi.isPresent()) {
      existingAmi.get().setId(id);
    } else {
      this.amis.add(AMI.builder().id(id).architecture(architecture).build());
    }
  }
}
