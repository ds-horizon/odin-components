package com.dream11.application.service;

import com.dream11.application.Application;
import com.dream11.application.exception.AsgNotFoundException;
import com.dream11.application.exception.LaunchTemplateNotFoundException;
import com.dream11.application.state.State;
import com.google.inject.Inject;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class StateCorrectionService {

  @NonNull final LaunchTemplateService launchTemplateService;

  @NonNull final AutoscalingGroupService autoscalingGroupService;

  public void correctState() {
    State state = Application.getState();
    this.correctAsgs(state);
    this.correctLts(state);
  }

  private void correctAsgs(State state) {
    List.copyOf(state.getAsg())
        .forEach(
            asg -> {
              try {
                this.autoscalingGroupService.describe(asg.getName());
              } catch (AsgNotFoundException ex) {
                log.warn("ASG:[{}] from state does not exist. Updating state.", asg.getName());
                state.removeAsgState(asg.getName());
              }
            });
  }

  private void correctLts(State state) {
    List.copyOf(state.getLt())
        .forEach(
            lt -> {
              try {
                this.launchTemplateService.describeLaunchTemplate(lt.getId());
              } catch (LaunchTemplateNotFoundException ex) {
                log.warn(
                    "Launch template:[{}] from state does not exist. Updating state.", lt.getId());
                state.removeLtState(lt.getId());
              }
            });
  }
}
